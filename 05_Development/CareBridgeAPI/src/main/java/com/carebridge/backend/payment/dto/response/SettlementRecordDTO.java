package com.carebridge.backend.payment.dto.response;

import com.carebridge.backend.expert.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Settlement record response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRecordDTO {

    private Long settlementId;
    private Long commissionId;
    private Long expertId;
    private LocalDate settlementPeriodStart;
    private LocalDate settlementPeriodEnd;
    private Integer grossAmount;
    private Integer commissionAmount;
    private Integer gatewayFee;
    private Integer refundAmount;
    private Integer expertNetAmount;
    private SettlementStatus status;
    private Instant settledAt;
    private String referenceCode;
    private Instant createdAt;
}
