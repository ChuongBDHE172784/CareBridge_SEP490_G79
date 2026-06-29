package com.carebridge.backend.expert.entity;

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
 * Expert review entity.
 * Stores user reviews and ratings for expert consultations.
 *
 * One review per consultation (enforced by unique constraint).
 */
@Entity
@Table(name = "expert_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    /**
     * Foreign key to consultation_bookings table.
     * One review per booking (enforced by unique constraint).
     */
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    /**
     * User who submitted the review.
     */
    @Column(name = "reviewer_user_id", nullable = false)
    private Long reviewerUserId;

    /**
     * Expert being reviewed.
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * Rating value (1-5 scale).
     */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /**
     * Optional comment/review text.
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Moderation status of the review.
     * PENDING -> APPROVED/REJECTED/HIDDEN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 50)
    private ModerationStatus moderationStatus;

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

    /**
     * Review moderation status enumeration.
     */
    public enum ModerationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        HIDDEN
    }
}
