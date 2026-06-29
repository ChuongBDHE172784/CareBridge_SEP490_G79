package com.carebridge.backend.payment.entity;

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
 * Contribution point entity.
 * Tracks loyalty/contribution points earned by users.
 *
 * Points can be earned from various activities:
 * - Consultation participation
 * - Community contributions
 * - Content creation
 * - Referral programs
 */
@Entity
@Table(name = "contribution_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionPoint {

    /**
     * Primary key - auto-generated point record ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_record_id")
    private Long pointRecordId;

    /**
     * Foreign key to users table.
     * ERD relationship: users ||--o{ contribution_points : "receives"
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Source type indicating what activity earned the points.
     * Example values: CONSULTATION, COMMUNITY_ANSWER, CONTENT_CREATION, REFERRAL
     */
    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    /**
     * Reference ID to the source entity.
     * Links to the specific record (booking_id, question_id, etc.) that earned points.
     */
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /**
     * Number of points earned/used.
     * Positive for earnings, negative for redemptions.
     */
    @Column(name = "points", nullable = false)
    private Integer points;

    /**
     * Reason or description for the point transaction.
     * Provides context for why points were awarded or deducted.
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * Timestamp when the points were recorded/earned.
     * This is the effective date of the point transaction.
     */
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /**
     * Creation timestamp.
     * Automatically set when the record is first inserted.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     * Automatically updated on any modification.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
