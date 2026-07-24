package com.carebridge.backend.exercise.entity;

import com.carebridge.backend.exercise.entity.converter.JsonbMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "maternal_observations")
@org.hibernate.annotations.SQLRestriction("legacy_source = 'EXERCISE_SAFETY'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSafetyCheck {

    @Id
    @Column(name = "observation_id", nullable = false)
    private UUID safetyCheckId;

    @Column(name = "exercise_template_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "mother_journey_id")
    private UUID journeyId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID userId;

    @Convert(converter = JsonbMapConverter.class)
    @Column(name = "payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Boolean> answerJson;

    @Column(name = "blocked_boolean", nullable = false)
    private Boolean redFlagDetected;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
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
    @Column(name = "schema_version", nullable = false, updatable = false, length = 30)
    private String schemaVersion = "1";

    @Builder.Default
    @Column(name = "source_type", nullable = false, updatable = false, length = 60)
    private String sourceType = "EXERCISE_SAFETY";

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "EXERCISE_SAFETY";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @PrePersist
    void prepareCanonicalObservation() {
        legacyId = safetyCheckId.toString();
        if (completedAt == null) {
            completedAt = createdAt;
        }
    }
}
