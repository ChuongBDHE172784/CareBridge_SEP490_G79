package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maternal_observations")
@org.hibernate.annotations.SQLRestriction("legacy_source = 'MATERNAL_METRIC'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaternalHealthMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "observation_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mother_journey_id", nullable = false)
    private UUID journeyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_type", nullable = false, length = 60)
    private MetricType metricType;

    @Column(name = "numeric_value")
    private BigDecimal valueNumeric;

    @Column(name = "secondary_numeric_value")
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "observed_at", nullable = false)
    private Instant measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private DataSource sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column(name = "text_value", columnDefinition = "text")
    private String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
    private MetricStatus status = MetricStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private String payloadJson = "{}";

    @Builder.Default
    @Column(name = "schema_version", nullable = false, updatable = false, length = 30)
    private String schemaVersion = "1";

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = "MATERNAL_METRIC";

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @PrePersist
    void prepareCanonicalObservation() {
        legacyId = id.toString();
        if (sourceType == null) {
            sourceType = DataSource.MANUAL;
        }
    }
}
