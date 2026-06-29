package com.carebridge.backend.payment.entity;

import com.carebridge.backend.expert.enums.SettlementStatus;
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
 * Refund record entity.
 * Tracks refund requests and their execution.
 *
 * Can be linked to a dispute or standalone.
 */
@Entity
@Table(name = "refund_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    /**
     * Foreign key to payment_transactions table.
     */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    /**
     * Optional link to consultation_disputes if refund originated from dispute.
     */
    @Column(name = "dispute_id")
    private Long disputeId;

    /**
     * Admin who approved the refund.
     */
    @Column(name = "approved_by", nullable = false)
    private Long approvedBy;

    /**
     * Amount to refund in smallest currency unit.
     * Can be partial or full.
     */
    @Column(name = "refund_amount", nullable = false)
    private Integer refundAmount;

    /**
     * Currency code.
     */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * Reason for the refund.
     */
    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    /**
     * Refund transaction ID from payment gateway.
     */
    @Column(name = "gateway_refund_id", length = 200)
    private String gatewayRefundId;

    /**
     * Refund processing status.
     */
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    /**
     * When the refund was requested.
     */
    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    /**
     * When the refund was actually processed.
     */
    @Column(name = "processed_at")
    private Instant processedAt;

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
