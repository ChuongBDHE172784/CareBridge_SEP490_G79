package com.carebridge.backend.payment.dto.response;

import com.carebridge.backend.expert.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Payment response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * Payment transaction ID.
     */
    private Long paymentId;

    /**
     * Booking reference.
     */
    private String bookingRef;

    /**
     * Gross amount.
     */
    private Integer grossAmount;

    /**
     * Gateway fee.
     */
    private Integer gatewayFee;

    /**
     * Net paid amount.
     */
    private Integer netPaidAmount;

    /**
     * Currency.
     */
    private String currency;

    /**
     * Payment status.
     */
    private PaymentStatus status;

    /**
     * Gateway transaction ID.
     */
    private String gatewayTransactionId;

    /**
     * When payment was completed.
     */
    private Instant paidAt;

    /**
     * Realtime session token for the consultation.
     */
    private String sessionToken;
}
