package com.carebridge.backend.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "health_observations")
@SQLRestriction("legacy_source = 'maternal_health_observations'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthObservation {

    public static final String CANONICAL_SOURCE = "maternal_health_observations";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "health_observation_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_subject_id", nullable = false)
    private UUID careSubjectId;

    @Column(name = "observation_type", nullable = false, length = 50)
    private String metricCode;

    @Column(name = "value_numeric", precision = 10, scale = 2)
    private BigDecimal valueNumeric;

    @Column(name = "value_secondary", precision = 10, scale = 2)
    private BigDecimal valueSecondary;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "observed_at", nullable = false)
    private Instant measuredAt;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> context = new LinkedHashMap<>();

    @Column(name = "original_unit", length = 30)
    private String originalUnit;

    @Column(name = "definition_version")
    private Integer definitionVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_shape", length = 30)
    private ObservationShape observationShape;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 60)
    private DataSource sourceType;

    // device_connection_id is gone with the IoT integration. Provenance for
    // historically device-sourced observations lives in raw_payload_jsonb under
    // "deviceProvenance"; sourceType still records that the reading came from a
    // device.

    @Column(name = "source_record_id")
    private UUID sourceRecordId;

    @Column(name = "quality_label", length = 30)
    private String qualityLabel;

    @Column(name = "text_value", columnDefinition = "text")
    private String note;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Builder.Default
    @Column(name = "legacy_source", nullable = false, updatable = false, length = 60)
    private String legacySource = CANONICAL_SOURCE;

    @Column(name = "legacy_id", nullable = false, updatable = false, length = 100)
    private String legacyId;

    @Builder.Default
    @Column(name = "subject_type", nullable = false, updatable = false, length = 30)
    private String subjectType = "MOTHER";

    /**
     * Ties together the observations produced by a single measuring session — a baby growth
     * entry records weight, height and head circumference at once and must stay one logical
     * aggregate (V3 §3.12). Null for observations that stand alone.
     *
     * <p>Immutable: regrouping an observation after the fact would silently move a reading
     * into another session.
     */
    @Column(name = "measurement_group_id", updatable = false)
    private UUID measurementGroupId;

    /**
     * Soft-delete marker. Every read path must filter on this; nothing else hides the row.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (legacyId == null || legacyId.isBlank()) legacyId = UUID.randomUUID().toString();
        if (legacySource == null || legacySource.isBlank()) legacySource = CANONICAL_SOURCE;
        if (subjectType == null || subjectType.isBlank()) subjectType = "MOTHER";
        if (sourceType == null) sourceType = DataSource.MANUAL;
        if (qualityLabel == null || qualityLabel.isBlank()) qualityLabel = "UNKNOWN";
        if (payload == null) payload = new LinkedHashMap<>();
        payload.putIfAbsent("recordStatus", MetricStatus.ACTIVE.name());
    }

    public MetricStatus recordStatus() {
        Object value = payload == null ? null : payload.get("recordStatus");
        return value == null ? MetricStatus.ACTIVE : MetricStatus.valueOf(value.toString());
    }

    public void setRecordStatus(MetricStatus status) {
        if (payload == null) payload = new LinkedHashMap<>();
        payload.put("recordStatus", status.name());
    }
}
