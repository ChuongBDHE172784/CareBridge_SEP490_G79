package com.carebridge.backend.payment.mapper;

import com.carebridge.backend.payment.dto.response.CommissionRecordDTO;
import com.carebridge.backend.payment.dto.response.PaymentResponse;
import com.carebridge.backend.payment.dto.response.SettlementRecordDTO;
import com.carebridge.backend.payment.entity.CommissionRecord;
import com.carebridge.backend.payment.entity.PaymentTransaction;
import com.carebridge.backend.payment.entity.SettlementRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Mapper for Payment module entities to DTOs.
 */
@Component
public class PaymentMapper {

    /**
     * Map PaymentTransaction to PaymentResponse.
     */
    public PaymentResponse toPaymentResponse(PaymentTransaction payment, String sessionToken) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingRef(payment.getBookingRef())
                .grossAmount(payment.getGrossAmount())
                .gatewayFee(payment.getGatewayFee())
                .netPaidAmount(payment.getNetPaidAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus() != null ?
                        com.carebridge.backend.expert.enums.PaymentStatus.valueOf(payment.getStatus().name()) : null)
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .paidAt(payment.getPaidAt())
                .sessionToken(sessionToken)
                .build();
    }

    /**
     * Map CommissionRecord to DTO.
     */
    public CommissionRecordDTO toCommissionDTO(CommissionRecord record) {
        return CommissionRecordDTO.builder()
                .commissionId(record.getCommissionId())
                .paymentId(record.getPaymentId())
                .bookingId(record.getBookingId())
                .expertId(record.getExpertId())
                .originalPrice(record.getOriginalPrice())
                .commissionRate(record.getCommissionRate())
                .commissionAmount(record.getCommissionAmount())
                .gatewayFee(record.getGatewayFee())
                .refundAmount(record.getRefundAmount())
                .expertNetAmount(record.getExpertNetAmount())
                .eligibleAt(record.getEligibleAt())
                .settlementStatus(record.getSettlementStatus())
                .settledAt(record.getSettledAt())
                .referenceCode(record.getReferenceCode())
                .createdAt(record.getCreatedAt())
                .build();
    }

    /**
     * Map SettlementRecord to DTO.
     */
    public SettlementRecordDTO toSettlementDTO(SettlementRecord record) {
        return SettlementRecordDTO.builder()
                .settlementId(record.getSettlementId())
                .commissionId(record.getCommissionId())
                .expertId(record.getExpertId())
                .settlementPeriodStart(record.getSettlementPeriodStart())
                .settlementPeriodEnd(record.getSettlementPeriodEnd())
                .grossAmount(record.getGrossAmount())
                .commissionAmount(record.getCommissionAmount())
                .gatewayFee(record.getGatewayFee())
                .refundAmount(record.getRefundAmount())
                .expertNetAmount(record.getExpertNetAmount())
                .status(record.getStatus() != null ?
                        com.carebridge.backend.expert.enums.SettlementStatus.valueOf(record.getStatus().name()) : null)
                .settledAt(record.getSettledAt())
                .referenceCode(record.getReferenceCode())
                .createdAt(record.getCreatedAt())
                .build();
    }

    /**
     * Map ConsultationPriceBand to DTO.
     */
    public com.carebridge.backend.payment.dto.response.PriceBandDTO toPriceBandDTO(
            com.carebridge.backend.payment.entity.ConsultationPriceBand band) {
        return com.carebridge.backend.payment.dto.response.PriceBandDTO.builder()
                .priceBandId(band.getPriceBandId())
                .configuredBy(band.getConfiguredBy())
                .channelType(band.getChannelType())
                .durationMinutes(band.getDurationMinutes())
                .specialtyScope(band.getSpecialtyScope())
                .minimumPrice(band.getMinimumPrice())
                .maximumPrice(band.getMaximumPrice())
                .commissionRate(band.getCommissionRate())
                .currency(band.getCurrency())
                .status(band.getStatus())
                .build();
    }

    /**
     * Map ExpertConsultationPrice to DTO.
     */
    public com.carebridge.backend.expert.dto.response.ExpertPriceSummary toExpertPriceSummary(
            com.carebridge.backend.payment.entity.ExpertConsultationPrice price) {
        return com.carebridge.backend.expert.dto.response.ExpertPriceSummary.builder()
                .expertPriceId(price.getExpertPriceId())
                .channelType(price.getChannelType())
                .durationMinutes(price.getDurationMinutes())
                .priceAmount(price.getPriceAmount())
                .currency(price.getCurrency())
                .status(price.getStatus())
                .build();
    }
}
