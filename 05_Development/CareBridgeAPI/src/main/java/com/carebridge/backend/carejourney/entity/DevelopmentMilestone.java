package com.carebridge.backend.carejourney.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "development_milestones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevelopmentMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "milestone_id", updatable = false, nullable = false)
    private UUID milestoneId;

    @Column(name = "baby_id", nullable = false)
    private UUID babyId;

    /**
     * Canonical care-subject identifier. The canonical table keeps both this
     * column and the legacy baby_id column non-null; the lifecycle callback
     * mirrors them because milestone APIs are baby-scoped.
     */
    @Column(name = "care_subject_id", nullable = false)
    private UUID careSubjectId;

    @Column(name = "milestone_type", nullable = false, length = 80)
    private String milestoneType;

    @Column(name = "achieved_date")
    private LocalDate achievedDate;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_status", nullable = false, length = 20)
    private MilestoneAchievementStatus milestoneStatus = MilestoneAchievementStatus.ACHIEVED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
    private MilestoneRecordStatus recordStatus = MilestoneRecordStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void alignCanonicalCareSubject() {
        if (careSubjectId == null && babyId != null) {
            careSubjectId = babyId;
        }
        if (babyId == null && careSubjectId != null) {
            babyId = careSubjectId;
        }
    }
}
