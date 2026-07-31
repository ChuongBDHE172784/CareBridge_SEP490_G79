package com.carebridge.backend.family.entity;

import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "FamilyCareTask")
@Table(name = "care_tasks")
@org.hibernate.annotations.SQLRestriction("task_type = 'MANUAL_TASK'")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_group_id", nullable = false)
    private UUID careGroupId;

    @Column(name = "creator_user_id")
    private UUID assignedBy;

    @Column(name = "assignee_user_id")
    private UUID assignedTo;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "scheduled_at")
    private Instant dueAt;

    @Builder.Default
    @Column(name = "task_type", nullable = false, updatable = false, length = 40)
    private String taskType = "MANUAL_TASK";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CareTaskStatus status = CareTaskStatus.OPEN;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** V2 provenance and explicit target; populated by the V2 task authoring path. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private ChecklistOrigin origin = ChecklistOrigin.USER_CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_subject", nullable = false, length = 10)
    private ChecklistTargetSubject targetSubject;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @PrePersist
    @PreUpdate
    void prepareUnifiedTaskMetadata() {
        if (origin == null) {
            origin = ChecklistOrigin.USER_CREATED;
        }
        if (targetSubject == null) {
            targetSubject = babyId != null
                    ? ChecklistTargetSubject.BABY
                    : ChecklistTargetSubject.MOTHER;
        }
    }
}
