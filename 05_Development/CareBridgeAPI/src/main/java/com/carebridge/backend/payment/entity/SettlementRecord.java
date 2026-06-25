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
import java.time.LocalDate;

/**
 * Settlement record entity.
 * Tracks expert commission settlements (payouts).
 *
 * Aggregates commission records into settlement periods.
 */
@Entity
@Table(name = "settlement_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long settlementId;

    /**
     * Foreign key to commission_records table.
     * One settlement per commission record.
     */
    @Column(name = "commission_id", nullable = false, unique = true)
    private Long commissionId;

    /**
     * Expert receiving the settlement.
     */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /**
     * Start date of the settlement period.
     */
    @Column(name = "settlement_period_start", nullable = false)
    private LocalDate settlementPeriodStart;

    /**
     * End date of the settlement period.
     */
    @Column(name = "settlement_period_end", nullable = false)
    private LocalDate settlementPeriodEnd;

    /**
     * Total gross amount before deductions.
     */
    @Column(name = "gross_amount", nullable = false)
    private Integer grossAmount;

    /**
     * Commission amount before refunds.
     */
    @Column(name = "commission_amount", nullable = false)
    private Integer commissionAmount;

    /**
     * Payment gateway fees deducted.
     */
    @Column(name = "gateway_fee", nullable = false)
    @Builder.Default
    private Integer gatewayFee = 0;

    /**
     * Refunds deducted from this settlement.
     */
    @Column(name = "refund_amount", nullable = false)
    @Builder.Default
    private Integer refundAmount = 0;

    /**
     * Net amount to be paid to the expert.
     * Calculated: commission_amount - gateway_fee - refund_amount.
     */
    @Column(name = "expert_net_amount", nullable = false)
    private Integer expertNetAmount;

    /**
     * Settlement status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SettlementStatus status;

    /**
     * When the settlement was actually paid.
     */
    @Column(name = "settled_at")
    private Instant settledAt;

    /**
     * External reference code (bank transfer ID, etc.).
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
