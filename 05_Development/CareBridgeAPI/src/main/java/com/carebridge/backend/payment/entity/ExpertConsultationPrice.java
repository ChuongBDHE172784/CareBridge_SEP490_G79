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
 * Expert consultation price entity.
 * Expert-specific pricing for consultation services.
 *
 * Can reference a price band for constraints.
 * Supports versioning with effective_from/effective_to.
 * ADR-EXP-005: Versioned pricing strategy.
 */
@Entity
@Table(name = "expert_consultation_prices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertConsultationPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_price_id")
    private Long expertPriceId;

    /**
     * Foreign key to experts table.
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * Optional link to price band for constraint validation.
     */
    @Column(name = "price_band_id")
    private Long priceBandId;

    /**
     * Consultation channel type.
     */
    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;

    /**
     * Consultation duration in minutes.
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * Expert's set price for this modality/duration.
     */
    @Column(name = "price_amount", nullable = false)
    private Integer priceAmount;

    /**
     * Currency code.
     */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * Cancellation policy text for this expert's service.
     */
    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    /**
     * When this price became effective.
     */
    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private Instant effectiveFrom = Instant.now();

    /**
     * When this price expires (null for current).
     */
    @Column(name = "effective_to")
    private Instant effectiveTo;

    /**
     * Status of this price entry.
     * ACTIVE, INACTIVE, DEPRECATED.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * Version number for optimistic locking and history.
     */
    @Column(name = "version_no", nullable = false)
    @Builder.Default
    private Integer versionNo = 1;

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
