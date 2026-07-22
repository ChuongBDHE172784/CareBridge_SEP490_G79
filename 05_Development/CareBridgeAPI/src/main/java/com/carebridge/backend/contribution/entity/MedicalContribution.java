package com.carebridge.backend.contribution.entity;

import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medical_contributions",
        indexes = {
                @Index(name = "idx_medical_contributions_expert_user_id", columnList = "expert_user_id"),
                @Index(name = "idx_medical_contributions_status", columnList = "status"),
                @Index(name = "idx_medical_contributions_specialty_id", columnList = "specialty_id"),
                @Index(name = "idx_medical_contributions_hospital_id", columnList = "hospital_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "contribution_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "expert_user_id", nullable = false)
    private UUID expertUserId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "specialty_id", length = 5)
    private String specialtyId;

    @Column(name = "hospital_id", length = 8)
    private String hospitalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ContributionStatus status = ContributionStatus.DRAFT;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}