package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "posture_analysis_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureAnalysisConfig {

    @Id
    @Column(name = "posture_config_id", nullable = false)
    private UUID postureConfigId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "configured_by", nullable = false)
    private UUID configuredBy;

    @Column(name = "analysis_mode", length = 30)
    private String analysisMode;

    @Column(name = "rule_or_model_version", length = 80)
    private String ruleOrModelVersion;

    @Column(name = "confidence_threshold")
    private BigDecimal confidenceThreshold;

    @Column(name = "feedback_level", length = 30)
    private String feedbackLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "status", length = 20)
    private String status;
}
