package com.carebridge.backend.exercise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posture_analysis_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureAnalysisConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "posture_config_id")
    private UUID postureConfigId;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "configured_by")
    private UUID configuredBy;

    @Column(name = "analysis_mode", length = 20)
    private String analysisMode;

    @Column(name = "rule_or_model_version", length = 100)
    private String ruleOrModelVersion;

    @Column(name = "confidence_threshold")
    private BigDecimal confidenceThreshold;

    @Column(name = "feedback_level", length = 20)
    private String feedbackLevel;

    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
