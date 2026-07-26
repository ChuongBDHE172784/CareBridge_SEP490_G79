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
@SQLRestriction("event_category = 'PREGNANCY_OUTCOME_EVIDENCE'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PregnancyOutcomeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_reference_id", nullable = false, updatable = false)
    private UUID journeyId;

    @Transient private UUID ownerUserId;

    @Transient private UUID submissionId;

    @Transient private PregnancyOutcomeType outcomeType;
    @Transient private LocalDate outcomeDate;

    @Builder.Default
    @Column(name = "resource_type", length = 100, updatable = false)
    private String resourceType = "mother_journeys";

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Transient private JourneyDateSource source;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Transient private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant effectiveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Transient private int revisionNumber;

    @Transient private UUID supersedesEvidenceId;

    @Transient private long journeyVersion;

    @Transient private String semanticHash;

    @Transient private boolean correction;

    @Builder.Default
    @Column(name = "event_category", nullable = false, updatable = false, length = 80)
    private String eventCategory = "PREGNANCY_OUTCOME_EVIDENCE";

    @Builder.Default
    @Transient
    private String eventOrigin = "PREGNANCY_OUTCOME_EVIDENCE";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    void prepareCanonicalEvent() {
        resourceId = journeyId;
        if (actorUserId == null) actorUserId = ownerUserId;
        if (ownerUserId == null) ownerUserId = actorUserId;
        Map<String, Object> value = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        value.put("outcomeType", outcomeType == null ? null : outcomeType.name());
        value.put("outcomeDate", outcomeDate == null ? null : outcomeDate.toString());
        value.put("revisionNumber", revisionNumber);
        value.put("journeyVersion", journeyVersion);
        value.put("correction", correction);
        value.put("source", source == null ? null : source.name());
        value.put("reason", reason);
        value.put("supersedesEvidenceId", supersedesEvidenceId);
        value.put("semanticHash", semanticHash);
        value.put("submissionId", submissionId);
        payload = value;
    }

    @PostLoad
    void hydrateCanonicalEvent() {
        ownerUserId = actorUserId;
        if (payload == null) return;
        outcomeType = enumValue(PregnancyOutcomeType.class, payload.get("outcomeType"));
        Object date = payload.get("outcomeDate");
        if (date != null && !date.toString().isBlank()) outcomeDate = LocalDate.parse(date.toString());
        revisionNumber = intValue(payload.get("revisionNumber"));
        // V312 legacy rows did not carry this value. Zero is the explicit
        // pre-versioning sentinel and prevents a missing payload key from
        // being mistaken for a current journey revision.
        journeyVersion = longValue(payload.getOrDefault("journeyVersion", 0L));
        correction = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("correction", false)));
        source = enumValue(JourneyDateSource.class, payload.get("source"));
        reason = text(payload.get("reason"));
        supersedesEvidenceId = uuid(payload.get("supersedesEvidenceId"));
        semanticHash = text(payload.get("semanticHash"));
        submissionId = uuid(payload.get("submissionId"));
    }

    @PreUpdate
    @PreRemove
    void rejectMutation() {
        throw new UnsupportedOperationException("Pregnancy outcome evidence is append-only");
    }

    private int intValue(Object value) {
        return value instanceof Number n ? n.intValue() : value == null ? 0 : Integer.parseInt(value.toString());
    }

    private long longValue(Object value) {
        return value instanceof Number n ? n.longValue() : value == null ? 0 : Long.parseLong(value.toString());
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        return value == null || value.toString().isBlank() ? null : Enum.valueOf(type, value.toString());
    }

    private String text(Object value) {
        return value == null ? null : value.toString();
    }

    private UUID uuid(Object value) {
        return value == null || value.toString().isBlank() ? null : UUID.fromString(value.toString());
    }
}
