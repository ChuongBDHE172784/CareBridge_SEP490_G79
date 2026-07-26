package com.carebridge.backend.aimoderation.entity;

import com.carebridge.backend.content.entity.ReportTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One AI evaluation of one content version. Idempotency key:
 * (targetType, targetId, contentHash, policySetHash, model) for COMPLETED rows.
 * Stores only structured rationale and short verified evidence excerpts — never the API key,
 * never chain-of-thought, never the full prompt/response.
 */
@Entity
@Table(name = "ai_content_assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiContentAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assessment_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "job_id", columnDefinition = "uuid")
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "uuid")
    private UUID targetId;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "policy_set_hash", nullable = false, length = 64)
    private String policySetHash;

    @Column(name = "provider", nullable = false, length = 30)
    @Builder.Default
    private String provider = "GEMINI";

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiAssessmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 20)
    private AiClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_severity", length = 20)
    private AiPolicySeverity overallSeverity;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 30)
    private AiRecommendedAction recommendedAction;

    @Column(name = "explanation", length = 1000)
    private String explanation;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 1;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    /**
     * CB-MOD-IMP-017: consolidated policy matches (was table ai_assessment_matches) — JSON
     * array of immutable snapshots {policyId, policyCode, policyVersion, category, severity,
     * confidence, evidence[], explanation}, written atomically with the assessment row.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matches_jsonb", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String matchesJson = "[]";

    @Column(name = "moderation_case_id", columnDefinition = "uuid")
    private UUID moderationCaseId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void initializeTimestamps() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
