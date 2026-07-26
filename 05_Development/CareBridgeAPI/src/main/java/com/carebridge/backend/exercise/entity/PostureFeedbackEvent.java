package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Table(name = "health_observations")
@SQLRestriction("observation_type = 'POSTURE_FEEDBACK'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureFeedbackEvent {

    @Id
    @Column(name = "health_observation_id", nullable = false)
    private UUID feedbackEventId;

    @Column(name = "source_record_id", nullable = false)
    private UUID exerciseSessionId;

    @Column(name = "care_subject_id", nullable = false)
    private UUID journeyId;

    @Transient private UUID postureConfigId;
    @Transient private Long eventTimeMs;
    @Transient private String postureCode;

    @Column(name = "value_numeric")
    private BigDecimal confidenceScore;

    @Column(name = "severity", length = 30)
    private String severity;

    @Column(name = "text_value", columnDefinition = "text")
    private String feedbackText;

    @Transient
    private String keypointSummaryJson;

    @CreationTimestamp
    @Column(name = "observed_at", nullable = false, updatable = false)
    private OffsetDateTime observedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder.Default
    @Column(name = "observation_type", nullable = false, updatable = false, length = 60)
    private String observationType = "POSTURE_FEEDBACK";

    @Builder.Default
    @Column(name = "subject_type", nullable = false, updatable = false, length = 30)
    private String subjectType = "MOTHER";

    @Builder.Default
    @Column(name = "unit", length = 40)
    private String unit = "CONFIDENCE";

    @Builder.Default
    @Column(name = "source_type", nullable = false, updatable = false, length = 60)
    private String sourceType = "POSTURE_ANALYSIS";

    @Builder.Default
    @Column(name = "legacy_source", updatable = false, length = 60)
    private String legacySource = "posture_feedback_events";

    @Column(name = "legacy_id", updatable = false, length = 100)
    private String legacyId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> canonicalPayload;

    @PrePersist
    @PreUpdate
    void prepareCanonicalObservation() {
        if (legacyId == null && feedbackEventId != null) legacyId = feedbackEventId.toString();
        Map<String, Object> payload = canonicalPayload == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(canonicalPayload);
        payload.put("eventTimeMs", eventTimeMs);
        payload.put("postureConfigId", postureConfigId);
        payload.put("postureCode", postureCode);
        if (keypointSummaryJson != null) payload.put("keypointSummary", keypointSummaryJson);
        canonicalPayload = payload;
    }

    @PostLoad
    void hydrateCanonicalObservation() {
        if (canonicalPayload == null) return;
        postureConfigId = uuid(canonicalPayload.get("postureConfigId"));
        Object time = canonicalPayload.get("eventTimeMs");
        if (time instanceof Number number) eventTimeMs = number.longValue();
        else if (time != null) eventTimeMs = Long.valueOf(time.toString());
        if (canonicalPayload.get("postureCode") != null) {
            postureCode = canonicalPayload.get("postureCode").toString();
        }
        if (canonicalPayload.get("keypointSummary") != null) {
            keypointSummaryJson = canonicalPayload.get("keypointSummary").toString();
        }
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        return value instanceof UUID id ? id : UUID.fromString(value.toString());
    }
}
