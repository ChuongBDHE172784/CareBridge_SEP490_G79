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

    @Builder.Default
    @Transient
    private String eventOrigin = "MOTHER_JOURNEY_TRANSITION";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    void prepareCanonicalEvent() {
        resourceId = journeyId;
        if (actorUserId == null) actorUserId = ownerUserId;
        if (ownerUserId == null) ownerUserId = actorUserId;
        Map<String, Object> value = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        value.put("eventType", eventType == null ? null : eventType.name());
        value.put("fromStage", fromStage == null ? null : fromStage.name());
        value.put("toStage", toStage == null ? null : toStage.name());
        value.put("changes", changes == null ? Map.of() : changes);
        value.put("journeyVersion", journeyVersion);
        value.put("source", source == null ? null : source.name());
        value.put("confidence", confidence == null ? null : confidence.name());
        value.put("reason", reason);
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
        if (version instanceof Number number) journeyVersion = number.longValue();
        else if (version != null) journeyVersion = Long.parseLong(version.toString());
        source = enumValue(JourneyDateSource.class, payload.get("source"));
        confidence = enumValue(JourneyDateConfidence.class, payload.get("confidence"));
        reason = payload.get("reason") == null ? null : payload.get("reason").toString();
        if (eventType == null) eventType = inferMigratedEventType();
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Mother journey transitions are append-only");
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        return value == null || value.toString().isBlank() ? null : Enum.valueOf(type, value.toString());
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
