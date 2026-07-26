package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "health_observations")
@SQLRestriction("observation_type = 'EXERCISE_SAFETY_RESULT'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSafetyCheck {

    @Id
    @Column(name = "health_observation_id", nullable = false)
    private UUID safetyCheckId;

    @Transient
    private UUID exerciseId;

    @Column(name = "care_subject_id", nullable = false)
    private UUID journeyId;

    @Transient
    private UUID userId;

    @Transient
    private Map<String, Boolean> answerJson;

    @Transient
    private Boolean redFlagDetected;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_label", length = 30)
    private SafetyCheckStatus resultStatus;

    @Column(name = "text_value", columnDefinition = "text")
    private String blockedReason;

    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Builder.Default
    @Column(name = "observation_type", nullable = false, updatable = false, length = 60)
    private String observationType = "EXERCISE_SAFETY_RESULT";

    @Builder.Default
    @Column(name = "subject_type", nullable = false, updatable = false, length = 30)
    private String subjectType = "MOTHER";

    @Builder.Default
    @Column(name = "source_type", nullable = false, updatable = false, length = 60)
    private String sourceType = "EXERCISE_SAFETY";

    @Builder.Default
    @Column(name = "legacy_source", updatable = false, length = 60)
    private String legacySource = "exercise_safety_checks";

    @Column(name = "legacy_id", updatable = false, length = 100)
    private String legacyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> canonicalPayload;

    @PrePersist
    @PreUpdate
    void prepareCanonicalObservation() {
        if (legacyId == null && safetyCheckId != null) legacyId = safetyCheckId.toString();
        if (completedAt == null) completedAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        Map<String, Object> payload = canonicalPayload == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(canonicalPayload);
        payload.put("exerciseTemplateId", exerciseId);
        payload.put("ownerUserId", userId);
        payload.put("answer", answerJson == null ? Map.of() : answerJson);
        payload.put("blockedBoolean", Boolean.TRUE.equals(redFlagDetected));
        payload.put("recordStatus", resultStatus == null ? null : resultStatus.name());
        canonicalPayload = payload;
    }

    @PostLoad
    @SuppressWarnings("unchecked")
    void hydrateCanonicalObservation() {
        if (canonicalPayload == null) return;
        exerciseId = uuid(canonicalPayload.get("exerciseTemplateId"));
        userId = uuid(canonicalPayload.get("ownerUserId"));
        redFlagDetected = Boolean.valueOf(String.valueOf(
                canonicalPayload.getOrDefault("blockedBoolean", false)));
        Object status = canonicalPayload.get("recordStatus");
        if (resultStatus == null && status != null) resultStatus = SafetyCheckStatus.valueOf(status.toString());
        Object answers = canonicalPayload.get("answer");
        if (answers instanceof Map<?, ?> map) {
            Map<String, Boolean> hydrated = new LinkedHashMap<>();
            map.forEach((key, value) -> hydrated.put(String.valueOf(key), Boolean.valueOf(String.valueOf(value))));
            answerJson = hydrated;
        }
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        return value instanceof UUID id ? id : UUID.fromString(value.toString());
    }
}
