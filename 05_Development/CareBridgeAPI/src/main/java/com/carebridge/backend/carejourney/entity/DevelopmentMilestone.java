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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
