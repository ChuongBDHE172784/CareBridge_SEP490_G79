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
@Table(name = "commission_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "commission_id")
    private UUID commissionId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "gateway_fee")
    private BigDecimal gatewayFee;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "expert_net_amount")
    private BigDecimal expertNetAmount;

    @Column(name = "eligible_at")
    private Instant eligibleAt;

    @Column(name = "settlement_status", length = 20)
    private String settlementStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
