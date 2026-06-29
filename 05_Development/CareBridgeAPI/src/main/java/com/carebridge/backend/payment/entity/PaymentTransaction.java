package com.carebridge.backend.payment.entity;

import com.carebridge.backend.expert.enums.PaymentStatus;
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
 * Payment transaction entity.
 * Records payment transactions for consultations.
 *
 * Linked to external payment gateway (VNPay, etc.).
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * Foreign key to consultation_bookings table.
     * ERD: booking_id
     */
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /**
     * Booking reference from consultations table.
     * Denormalized for quick lookup.
     */
    @Column(name = "booking_ref", nullable = false, length = 100)
    private String bookingRef;

    /**
     * User who made the payment.
     */
    @Column(name = "payer_user_id", nullable = false)
    private Long payerUserId;

    /**
     * Payment gateway name (VNPay, Momo, etc.).
     */
    @Column(name = "gateway_name", nullable = false, length = 100)
    private String gatewayName;

    /**
     * Transaction ID from the payment gateway.
     */
    @Column(name = "gateway_transaction_id", length = 200)
    private String gatewayTransactionId;

    /**
     * Gross payment amount in smallest currency unit (VND).
     */
    @Column(name = "gross_amount", nullable = false)
    private Integer grossAmount;

    /**
     * Fee charged by the payment gateway.
     */
    @Column(name = "gateway_fee", nullable = false)
    @Builder.Default
    private Integer gatewayFee = 0;

    /**
     * Total refunded amount.
     */
    @Column(name = "refund_amount", nullable = false)
    @Builder.Default
    private Integer refundAmount = 0;

    /**
     * Net amount after gateway fees (gross - gateway_fee).
     */
    @Column(name = "net_paid_amount", nullable = false)
    private Integer netPaidAmount;

    /**
     * Currency code.
     */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * Payment status.
     * PENDING -> PROCESSING -> COMPLETED/FAILED.
     * Can transition to REFUNDED/PARTIALLY_REFUNDED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    /**
     * When the payment was completed.
     */
    @Column(name = "paid_at")
    private Instant paidAt;

    /**
     * When the payment was refunded.
     */
    @Column(name = "refunded_at")
    private Instant refundedAt;

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
