package com.carebridge.backend.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refund_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_id")
    private UUID refundId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "dispute_id")
    private UUID disputeId;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "gateway_refund_id", length = 150)
    private String gatewayRefundId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
