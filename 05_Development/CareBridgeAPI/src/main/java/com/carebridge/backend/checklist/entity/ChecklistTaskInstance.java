package com.carebridge.backend.checklist.entity;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistTaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
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
@Table(name = "checklist_task_instances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTaskInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checklist_task_instance_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "checklist_instance_id", nullable = false)
    private UUID checklistInstanceId;

    @Column(name = "template_version_id")
    private UUID templateVersionId;

    @Column(name = "template_item_version_id")
    private UUID templateItemVersionId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "task_key", nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String taskKey;

    @Builder.Default
    @Column(name = "key_version", nullable = false, length = 10)
    private String keyVersion = "v1";

    @Column(name = "title_snapshot", nullable = false, length = 500)
    private String titleSnapshot;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(name = "is_required", nullable = false)
    private Boolean required = Boolean.FALSE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private ChecklistCategory category = ChecklistCategory.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_subject", nullable = false, length = 10)
    private ChecklistTargetSubject targetSubject;

    @Column(name = "due_at")
    private Instant dueAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChecklistTaskStatus status = ChecklistTaskStatus.PENDING;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "skipped_at")
    private Instant skippedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "action_reason_code", length = 80)
    private String actionReasonCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
