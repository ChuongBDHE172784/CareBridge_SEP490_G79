package com.carebridge.backend.content.entity;

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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "moderation_cases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "moderation_case_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "target_id", columnDefinition = "uuid")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private ReportTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "reason_code", length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_source", nullable = false, length = 20)
    @Builder.Default
    private ReportSource reportSource = ReportSource.USER;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reporter_user_id", columnDefinition = "uuid")
    private UUID reporterUserId;

    @Column(name = "assigned_moderator_id", columnDefinition = "uuid")
    private UUID assignedModeratorId;

    // CB-MOD-IMP-016: review priority (AI decision policy may raise it; users default NORMAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private CasePriority priority = CasePriority.NORMAL;

    // CB-MOD-IMP-016: set on claim (IN_REVIEW), cleared on release
    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "opened_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // CB-MOD-IMP-017: CURRENT (latest) moderator feedback on the linked AI assessment —
    // consolidated from the dropped ai_assessment_feedback table. Full history is append-only
    // in audit_events (event_category = MODERATION_AI_FEEDBACK_SUBMITTED).
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_feedback_decision", length = 20)
    private com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict aiFeedbackDecision;

    @Column(name = "ai_feedback_reason", columnDefinition = "TEXT")
    private String aiFeedbackReason;

    @Column(name = "ai_feedback_by", columnDefinition = "uuid")
    private UUID aiFeedbackBy;

    @Column(name = "ai_feedback_at")
    private Instant aiFeedbackAt;

    @Column(name = "ai_feedback_assessment_id", columnDefinition = "uuid")
    private UUID aiFeedbackAssessmentId;

    // Legacy audit metadata retained for reports reverted before that workflow was removed.
    // No active service writes these fields; keeping them avoids a destructive data migration.
    @Column(name = "reverted_at")
    private Instant revertedAt;

    @Column(name = "reverted_by", columnDefinition = "uuid")
    private UUID revertedBy;

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (priority == null) {
            priority = CasePriority.NORMAL;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
