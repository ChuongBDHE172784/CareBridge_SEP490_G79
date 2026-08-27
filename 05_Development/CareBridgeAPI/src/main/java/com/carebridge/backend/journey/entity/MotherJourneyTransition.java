package com.carebridge.backend.journey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@org.hibernate.annotations.Immutable // append-only event row: never dirty-checked or updated
@Table(name = "audit_events")
@SQLRestriction("event_category = 'MOTHER_JOURNEY_TRANSITION'")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MotherJourneyTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_reference_id", nullable = false)
    private UUID journeyId;

    @Transient private JourneyTransitionType eventType;
    @Transient private JourneyType fromStage;
    @Transient private JourneyType toStage;
    @Transient private Map<String, Object> changes;
    @Transient private GestationalDatingBasis gestationalDatingBasis;
    @Transient private Long gestationalDatingRevision;
    @Transient private LocalDate canonicalLmp;
    @Transient private Boolean inferredSource;
    /** Legacy V1 compatibility object may retain STAGE_CHANGED in memory while
     * the canonical persisted payload is typed as an epoch event. */
    @Transient private Boolean pregnancyEpochStarted;
    @Transient private Long supersedesDatingRevision;

    @Builder.Default
    @Column(name = "resource_type", length = 100, updatable = false)
    private String resourceType = "mother_journeys";

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Transient private JourneyDateSource source;
    @Transient private JourneyDateConfidence confidence;
    @Transient private String reason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "correlation_id", updatable = false)
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant effectiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Transient private long journeyVersion;

    /**
     * Compatibility view for the former transition owner column.  Canonical audit rows use the
     * actor as the owner and V312 intentionally leaves subject_user_id empty.
     */
    @Transient private UUID ownerUserId;

    @Builder.Default
    @Column(name = "event_category", nullable = false, updatable = false, length = 80)
    private String eventCategory = "MOTHER_JOURNEY_TRANSITION";

    /**
     * Journey events share audit_events with real audit logs. A distinct origin keeps
     * them out of the {@code AuditLog} entity (whose event_category maps to the
     * {@code AuditAction} enum) instead of relying on the DB default 'AUDIT_LOG'.
     */
    @Builder.Default
    @Column(name = "event_origin", nullable = false, updatable = false, length = 40)
    private String eventOrigin = "JOURNEY_EVENT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    void prepareCanonicalEvent() {
        eventOrigin = "JOURNEY_EVENT";
        if (reason != null && reason.length() > 500) {
            // Restores the legacy mother_journey_transitions.reason varchar(500)
            // guarantee that the canonical jsonb payload no longer enforces.
            throw new IllegalArgumentException(
                    "Journey transition reason must not exceed 500 characters");
        }
        resourceId = journeyId;
        if (actorUserId == null) actorUserId = ownerUserId;
        if (ownerUserId == null) ownerUserId = actorUserId;
        if (correlationId == null) correlationId = UUID.randomUUID();
        Map<String, Object> value = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        value.put("eventType", Boolean.TRUE.equals(pregnancyEpochStarted)
                ? JourneyTransitionType.PREGNANCY_EPOCH_STARTED.name()
                : eventType == null ? null : eventType.name());
        value.put("fromStage", fromStage == null ? null : fromStage.name());
        value.put("toStage", toStage == null ? null : toStage.name());
        value.put("changes", changes == null ? Map.of() : changes);
        value.put("journeyVersion", journeyVersion);
        value.put("source", source == null ? null : source.name());
        value.put("confidence", confidence == null ? null : confidence.name());
        value.put("reason", reason);
        if (gestationalDatingRevision != null || Boolean.TRUE.equals(pregnancyEpochStarted)) {
            value.put("gestationalDatingRevision", gestationalDatingRevision);
            value.put("basis", gestationalDatingBasis == null
                    ? null : gestationalDatingBasis.name());
            value.put("canonicalLmp", canonicalLmp == null ? null : canonicalLmp.toString());
            value.put("effectiveFrom", effectiveAt == null ? null : effectiveAt.toString());
            if (supersedesDatingRevision != null) {
                value.put("supersedesDatingRevision", supersedesDatingRevision);
            }
        }
        if (inferredSource != null) value.put("inferredSource", inferredSource);
        if (recordedAt != null) value.put("recordedAt", recordedAt.toString());
        value.put("correlationId", correlationId.toString());
        payload = value;
    }

    @PostLoad
    @SuppressWarnings("unchecked")
    void hydrateCanonicalEvent() {
        ownerUserId = actorUserId;
        if (payload == null) {
            changes = new LinkedHashMap<>();
            return;
        }
        eventType = enumValue(JourneyTransitionType.class, payload.get("eventType"));
        fromStage = enumValue(JourneyType.class, payload.get("fromStage"));
        toStage = enumValue(JourneyType.class, payload.get("toStage"));
        Object changed = payload.get("changes");
        changes = changed instanceof Map<?, ?> map ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
        Object version = payload.get("journeyVersion");
        journeyVersion = longValue(version);
        source = enumValue(JourneyDateSource.class, payload.get("source"));
        confidence = enumValue(JourneyDateConfidence.class, payload.get("confidence"));
        reason = payload.get("reason") == null ? null : payload.get("reason").toString();
        Object datingRevision = payload.get("gestationalDatingRevision");
        gestationalDatingRevision = nullableLongValue(datingRevision);
        gestationalDatingBasis = enumValue(GestationalDatingBasis.class, payload.get("basis"));
        Object lmp = payload.get("canonicalLmp");
        canonicalLmp = localDateValue(lmp);
        Object inferred = payload.get("inferredSource");
        inferredSource = inferred == null ? null : Boolean.valueOf(inferred.toString());
        Object supersedes = payload.get("supersedesDatingRevision");
        supersedesDatingRevision = nullableLongValue(supersedes);
        Object correlation = payload.get("correlationId");
        if (correlationId == null) correlationId = uuidValue(correlation);
        if (eventType == null) eventType = inferMigratedEventType();
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Mother journey transitions are append-only");
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try {
            return Enum.valueOf(type, value.toString());
        } catch (IllegalArgumentException exception) {
            // Legacy/migrated payloads are untrusted input.  Keep the event
            // readable and let callers fail closed on the missing projection.
            return null;
        }
    }

    private long longValue(Object value) {
        if (value == null) return 0L;
        try {
            long parsed = value instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(value.toString().trim());
            return parsed < 0 ? 0L : parsed;
        } catch (ArithmeticException | NumberFormatException exception) {
            return 0L;
        }
    }

    private Long nullableLongValue(Object value) {
        if (value == null) return null;
        long parsed = longValue(value);
        return parsed == 0L && !"0".equals(value.toString().trim()) ? null : parsed;
    }

    private LocalDate localDateValue(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try {
            return LocalDate.parse(value.toString());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private UUID uuidValue(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private JourneyTransitionType inferMigratedEventType() {
        if (fromStage != null && toStage != null && fromStage != toStage) {
            return JourneyTransitionType.STAGE_CHANGED;
        }
        if (changes.containsKey("status")) return JourneyTransitionType.STATUS_CHANGED;
        if (changes.keySet().stream().anyMatch(key ->
                key.toLowerCase(java.util.Locale.ROOT).contains("date")
                        || key.equals("source")
                        || key.equals("confidence"))) {
            return JourneyTransitionType.DATES_CHANGED;
        }
        return JourneyTransitionType.DETAILS_CHANGED;
    }
}
