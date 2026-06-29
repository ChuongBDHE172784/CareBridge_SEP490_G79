package com.carebridge.backend.expert.entity;

import com.carebridge.backend.expert.enums.ExpertVerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Expert profile entity.
 * Maps to experts table.
 *
 * Bounded Context: Expert Consultation
 * Data Classification: PII
 */
@Entity
@Table(name = "expert_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expertId;

    /**
     * Link to users table - one-to-one relationship.
     * Each verified user can have at most one expert profile.
     * ADR-EXP-001: One-to-one relationship for clear ownership.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * Medical specialty (e.g., OBSTETRICS, PEDIATRIC, CARDIOLOGY).
     * Required field for expert identification.
     */
    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    /**
     * Years of professional experience.
     * Must be non-negative.
     */
    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    /**
     * Professional title (e.g., "BS. CKII", "Dr.", "MD").
     */
    @Column(name = "professional_title", length = 200)
    private String professionalTitle;

    /**
     * Workplace/hospital/clinic name.
     */
    @Column(name = "workplace", length = 300)
    private String workplace;

    /**
     * Scope of consultation services offered.
     */
    @Column(name = "consultation_scope", columnDefinition = "TEXT")
    private String consultationScope;

    /**
     * Current verification status of the expert profile.
     * Follows state machine: PENDING_VERIFICATION -> APPROVED/REJECTED/SUSPENDED
     * ADR-EXP-002: State machine for verification workflow.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private ExpertVerificationStatus verificationStatus;

    /**
     * Timestamp when the expert profile was verified/approved.
     */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    /**
     * Admin user who verified this expert profile.
     * Foreign key to users table.
     */
    @Column(name = "verified_by")
    private Long verifiedBy;

    /**
     * Average rating from user reviews (1-5 scale).
     * Default: 0.00
     */
    @Column(name = "rating_avg", precision = 3, scale = 2)
    @Builder.Default
    private Double ratingAvg = 0.00;

    /**
     * Total number of reviews received.
     */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
