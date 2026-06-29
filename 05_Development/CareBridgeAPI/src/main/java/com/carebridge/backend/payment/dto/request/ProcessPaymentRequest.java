package com.carebridge.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    /**
     * Booking reference.
     */
    @NotBlank(message = "Booking reference is required")
    private String bookingRef;

    /**
     * Payment method (VNPAY, MOMO, etc.).
     */
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    /**
     * Transaction token from payment provider.
     */
    @NotBlank(message = "Transaction token is required")
    private String transactionToken;
}
