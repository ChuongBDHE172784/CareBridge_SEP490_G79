package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "maternal_observations")
@org.hibernate.annotations.SQLRestriction("legacy_source = 'POSTURE_FEEDBACK'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureFeedbackEvent {

    @Id
    @Column(name = "observation_id", nullable = false)
    private UUID feedbackEventId;

    @Column(name = "exercise_session_id", nullable = false)
    private UUID exerciseSessionId;

    @Column(name = "posture_config_id")
    private UUID postureConfigId;

    @Column(name = "event_time_ms", nullable = false)
    private Long eventTimeMs;

    @Column(name = "posture_code", length = 80)
    private String postureCode;

    @Column(name = "numeric_value")
    private BigDecimal confidenceScore;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "text_value", columnDefinition = "text")
    private String feedbackText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private String keypointSummaryJson;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "observed_at", nullable = false, updatable = false)
    private java.time.OffsetDateTime observedAt;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.OffsetDateTime createdAt;

    @Builder.Default
    @Column(name = "observation_type", nullable = false, updatable = false, length = 60)
    private String observationType = "POSTURE_FEEDBACK";

    @Builder.Default
    @Column(name = "source_type", nullable = false, updatable = false, length = 60)
    private String sourceType = "POSTURE_ANALYSIS";

    @Builder.Default
    @Column(name = "schema_version", nullable = false, updatable = false, length = 30)
    private String schemaVersion = "1";

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "POSTURE_FEEDBACK";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @PrePersist
    void prepareCanonicalObservation() {
        legacyId = feedbackEventId.toString();
        if (keypointSummaryJson == null) {
            keypointSummaryJson = "{}";
        }
    }
}
