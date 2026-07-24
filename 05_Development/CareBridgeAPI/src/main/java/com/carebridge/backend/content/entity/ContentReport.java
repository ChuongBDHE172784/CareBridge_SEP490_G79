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

    @Column(name = "opened_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // CB-MOD-IMP-015: most recent revert-to-PENDING event. resolvedAt/assignedModeratorId are
    // deliberately left untouched by revert (ADR-005) — these two fields are the only trace of it.
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
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
