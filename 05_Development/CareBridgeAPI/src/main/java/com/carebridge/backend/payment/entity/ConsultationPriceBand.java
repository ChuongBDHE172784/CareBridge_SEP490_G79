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
 * Consultation price band entity.
 * Admin-configured price bands for different consultation types.
 *
 * Used to validate expert prices and enforce min/max bounds.
 * ADR-EXP-005: Price versioning and band constraints.
 */
@Entity
@Table(name = "consultation_price_bands")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationPriceBand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_band_id")
    private Long priceBandId;

    /**
     * Admin who configured this price band.
     */
    @Column(name = "configured_by", nullable = false)
    private Long configuredBy;

    /**
     * Consultation channel type (CHAT, VOICE, VIDEO).
     */
    @Column(name = "channel_type", nullable = false, length = 50)
    private String channelType;

    /**
     * Consultation duration in minutes.
     */
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    /**
     * Optional specialty scope filter (can be null for all specialties).
     * E.g., "OBSTETRICS", "PEDIATRIC".
     */
    @Column(name = "specialty_scope", length = 200)
    private String specialtyScope;

    /**
     * Minimum allowed price for this band.
     */
    @Column(name = "minimum_price", nullable = false)
    private Integer minimumPrice;

    /**
     * Maximum allowed price for this band.
     */
    @Column(name = "maximum_price", nullable = false)
    private Integer maximumPrice;

    /**
     * Commission rate for this band (e.g., 0.20 for 20%).
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private Double commissionRate;

    /**
     * Currency code.
     */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * When this price band becomes effective.
     */
    @Column(name = "effective_from", nullable = false)
    @Builder.Default
    private Instant effectiveFrom = Instant.now();

    /**
     * When this price band expires (null for indefinite).
     */
    @Column(name = "effective_to")
    private Instant effectiveTo;

    /**
     * Status of the price band.
     * ACTIVE, INACTIVE, DEPRECATED.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";

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
