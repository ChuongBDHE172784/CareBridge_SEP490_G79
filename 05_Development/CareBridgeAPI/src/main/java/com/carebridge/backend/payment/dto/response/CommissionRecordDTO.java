package com.carebridge.backend.payment.dto.response;

import com.carebridge.backend.expert.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Commission record DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionRecordDTO {

    private Long commissionId;
    private Long paymentId;
    private Long bookingId;
    private Long expertId;
    private Integer originalPrice;
    private Double commissionRate;
    private Integer commissionAmount;
    private Integer gatewayFee;
    private Integer refundAmount;
    private Integer expertNetAmount;
    private Instant eligibleAt;
    private String settlementStatus;
    private Instant settledAt;
    private String referenceCode;
    private Instant createdAt;
}
