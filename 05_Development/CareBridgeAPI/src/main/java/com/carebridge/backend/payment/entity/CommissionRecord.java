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
 * Commission record entity.
 * Tracks commission earned by experts from completed payments.
 *
 * Used for settlement and payout tracking.
 */
@Entity
@Table(name = "commission_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commission_id")
    private Long commissionId;

    /**
     * Foreign key to payment_transactions table.
     */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    /**
     * Foreign key to consultation_bookings table.
     */
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /**
     * Expert who earned the commission.
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * Original consultation price before commission.
     */
    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    /**
     * Commission rate (e.g., 0.20 for 20%).
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private Double commissionRate;

    /**
     * Commission amount in currency units.
     * Calculated: original_price * commission_rate.
     */
    @Column(name = "commission_amount", nullable = false)
    private Integer commissionAmount;

    /**
     * Payment gateway fee portion allocated to this commission.
     */
    @Column(name = "gateway_fee", nullable = false)
    @Builder.Default
    private Integer gatewayFee = 0;

    /**
     * Portion of commission refunded (if any).
     */
    @Column(name = "refund_amount", nullable = false)
    @Builder.Default
    private Integer refundAmount = 0;

    /**
     * Net commission after refunds.
     * Calculated: commission_amount - refund_amount - gateway_fee.
     */
    @Column(name = "expert_net_amount", nullable = false)
    private Integer expertNetAmount;

    /**
     * When this commission becomes eligible for settlement.
     * Typically after the consultation's refund period expires.
     */
    @Column(name = "eligible_at")
    private Instant eligibleAt;

    /**
     * Settlement status.
     * PENDING -> ELIGIBLE -> SETTLED/CANCELLED.
     */
    @Column(name = "settlement_status", nullable = false, length = 50)
    @Builder.Default
    private String settlementStatus = "PENDING";

    /**
     * When the commission was actually settled/paid.
     */
    @Column(name = "settled_at")
    private Instant settledAt;

    /**
     * External settlement reference (bank transfer ID, etc.).
     */
    @Column(name = "reference_code", length = 200)
    private String referenceCode;

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
