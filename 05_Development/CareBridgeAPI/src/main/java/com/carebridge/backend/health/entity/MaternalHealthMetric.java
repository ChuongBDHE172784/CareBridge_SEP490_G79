package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "health_observations")
@org.hibernate.annotations.SQLRestriction("legacy_source = 'maternal_health_metrics'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaternalHealthMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "health_observation_id", updatable = false, nullable = false)
    private UUID id;

    @Transient
    private UUID journeyId;

    @Column(name = "care_subject_id", nullable = false)
    private UUID careSubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_type", nullable = false, length = 60)
    private MetricType metricType;

    @Column(name = "value_numeric")
    private BigDecimal valueNumeric;

    @Column(name = "value_secondary")
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "observed_at", nullable = false)
    private Instant measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private DataSource sourceType;

    @Transient
    private UUID sourceReferenceId;

    @Column(name = "text_value", columnDefinition = "text")
    private String note;

    @Builder.Default
    @Transient
    private MetricStatus status = MetricStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "raw_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payloadJson = new LinkedHashMap<>();

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "maternal_health_metrics";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @Builder.Default
    @Column(name = "subject_type", nullable = false, updatable = false, length = 30)
    private String subjectType = "MOTHER";

    @PrePersist
    @PreUpdate
    void prepareCanonicalObservation() {
        if (legacyId == null && id != null) legacyId = id.toString();
        if (sourceType == null) {
            sourceType = DataSource.MANUAL;
        }
        if (payloadJson == null) payloadJson = new LinkedHashMap<>();
        putPayload("journeyId", journeyId);
        putPayload("sourceReferenceId", sourceReferenceId);
        putPayload("recordStatus", status == null ? null : status.name());
    }

    @PostLoad
    void hydrateCanonicalObservation() {
        if (payloadJson == null) return;
        Object journey = payloadJson.get("journeyId");
        journeyId = journey == null || journey.toString().isBlank()
                ? null : UUID.fromString(journey.toString());
        Object reference = payloadJson.get("sourceReferenceId");
        sourceReferenceId = reference == null || reference.toString().isBlank()
                ? null : UUID.fromString(reference.toString());
        Object recordStatus = payloadJson.get("recordStatus");
        status = recordStatus == null || recordStatus.toString().isBlank()
                ? MetricStatus.ACTIVE : MetricStatus.valueOf(recordStatus.toString());
    }

    private void putPayload(String key, Object value) {
        if (value == null) payloadJson.remove(key); else payloadJson.put(key, value);
    }

}
