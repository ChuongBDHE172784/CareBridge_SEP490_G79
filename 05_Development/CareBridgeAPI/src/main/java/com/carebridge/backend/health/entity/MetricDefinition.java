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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "health_metric_definitions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_definition_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "metric_code", nullable = false, length = 60)
    private String metricCode;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_shape", nullable = false, length = 30)
    private ObservationShape observationShape;

    @Builder.Default
    @Column(name = "subject_type", nullable = false, length = 30)
    private String subjectType = "MOTHER";

    @Builder.Default
    @Column(name = "manual_entry_supported", nullable = false)
    private boolean manualEntrySupported = false;

    @Builder.Default
    @Column(name = "device_import_supported", nullable = false)
    private boolean deviceImportSupported = false;

    @Column(name = "canonical_unit", length = 30)
    private String canonicalUnit;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accepted_input_units_jsonb", nullable = false, columnDefinition = "jsonb")
    private List<String> acceptedInputUnits = new ArrayList<>();

    @Column(name = "precision_scale")
    private Short precisionScale;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_context_schema_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requiredContextSchema = new LinkedHashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plausibility_policy_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> plausibilityPolicy = new LinkedHashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aggregation_policy_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> aggregationPolicy = new LinkedHashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chart_policy_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> chartPolicy = new LinkedHashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quality_policy_jsonb", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> qualityPolicy = new LinkedHashMap<>();

    @Column(name = "safety_policy_version", length = 40)
    private String safetyPolicyVersion;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_journey_stages_jsonb", nullable = false, columnDefinition = "jsonb")
    private List<String> allowedJourneyStages = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (subjectType == null || subjectType.isBlank()) subjectType = "MOTHER";
        if (acceptedInputUnits == null) acceptedInputUnits = new ArrayList<>();
        if (requiredContextSchema == null) requiredContextSchema = new LinkedHashMap<>();
        if (plausibilityPolicy == null) plausibilityPolicy = new LinkedHashMap<>();
        if (aggregationPolicy == null) aggregationPolicy = new LinkedHashMap<>();
        if (chartPolicy == null) chartPolicy = new LinkedHashMap<>();
        if (qualityPolicy == null) qualityPolicy = new LinkedHashMap<>();
        if (allowedJourneyStages == null) allowedJourneyStages = new ArrayList<>();
        if (effectiveFrom == null) effectiveFrom = Instant.now();
    }
}
