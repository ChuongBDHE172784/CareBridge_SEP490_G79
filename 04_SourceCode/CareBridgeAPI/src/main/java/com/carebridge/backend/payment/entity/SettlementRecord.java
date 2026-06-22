package com.carebridge.backend.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "settlement_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "settlement_id")
    private UUID settlementId;

    @Column(name = "commission_id")
    private UUID commissionId;

    @Column(name = "expert_profile_id")
    private UUID expertProfileId;

    @Column(name = "settlement_period_start")
    private LocalDate settlementPeriodStart;

    @Column(name = "settlement_period_end")
    private LocalDate settlementPeriodEnd;

    @Column(name = "gross_amount")
    private BigDecimal grossAmount;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "gateway_fee")
    private BigDecimal gatewayFee;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "expert_net_amount")
    private BigDecimal expertNetAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
