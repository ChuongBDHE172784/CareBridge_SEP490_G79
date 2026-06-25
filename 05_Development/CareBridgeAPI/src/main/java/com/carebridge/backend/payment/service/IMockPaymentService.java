package com.carebridge.backend.payment.service;

/**
 * Mock Payment Service Interface.
 * Provides mock payment operations for Sprint 0 development.
 *
 * ISP: Interface segregation - mock-specific operations.
 */
public interface IMockPaymentService {

    /**
     * Generate a mock QR code for payment.
     *
     * @param amount the payment amount
     * @param currency the currency code
     * @return QR code URL
     */
    String generateQrCode(int amount, String currency);

    /**
     * Verify a mock payment transaction.
     *
     * @param transactionToken the transaction token
     * @return true if payment is verified (always true in mock)
     */
    boolean verifyPayment(String transactionToken);

    /**
     * Process a mock refund.
     *
     * @param originalTransactionId the original transaction ID
     * @param refundAmount the refund amount
     * @param reason the refund reason
     * @return mock refund confirmation
     */
    String processRefund(String originalTransactionId, int refundAmount, String reason);
}
