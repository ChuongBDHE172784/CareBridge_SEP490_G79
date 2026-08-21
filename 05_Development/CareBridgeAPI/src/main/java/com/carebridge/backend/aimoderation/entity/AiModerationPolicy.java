package com.carebridge.backend.aimoderation.entity;

import com.carebridge.backend.content.entity.ReportCategory;
import com.carebridge.backend.content.entity.ReportTargetType;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI content-moderation policy: validated data, not a free-form system prompt.
 * System-default policies are never hard-deleted; changes that affect classification bump
 * {@link #version} and are audited. Emergency triage is intentionally handled elsewhere.
 */
@Entity
@Table(name = "ai_moderation_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModerationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "policy_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "policy_code", nullable = false, length = 60, unique = true)
    private String policyCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "detection_guidance", nullable = false, columnDefinition = "TEXT")
    private String detectionGuidance;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_category", nullable = false, length = 40)
    private AiViolationCategory violationCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_category", nullable = false, length = 40)
    private ReportCategory reportCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AiPolicySeverity severity;

    /** CSV subset of QUESTION,ANSWER — kept as varchar for H2 test parity. */
    @Column(name = "applicable_target_types", nullable = false, length = 100)
    private String applicableTargetTypes;

    @Column(name = "confidence_threshold", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidenceThreshold;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "system_default", nullable = false)
    @Builder.Default
    private boolean systemDefault = false;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @Column(name = "reference_links", columnDefinition = "TEXT")
    private String referenceLinks;

    @Column(name = "reference_files", columnDefinition = "TEXT")
    private String referenceFiles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public List<ReportTargetType> targetTypes() {
        if (applicableTargetTypes == null || applicableTargetTypes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(applicableTargetTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ReportTargetType::valueOf)
                .toList();
    }

    public boolean appliesTo(ReportTargetType targetType) {
        return targetTypes().contains(targetType);
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
