package com.carebridge.backend.payment.service;

import com.carebridge.backend.payment.entity.PaymentTransaction;
import com.carebridge.backend.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Mock Payment Service Implementation.
 * Sprint 0: Mock provider for payment operations.
 *
 * Implements IMockPaymentService for mock payment operations.
 * Real implementation will integrate with VNPay in Sprint 1+.
 */
@Service("mockPaymentService")
@RequiredArgsConstructor
@Slf4j
public class MockPaymentServiceImpl implements IMockPaymentService {

    private final PaymentTransactionRepository paymentRepository;

    @Override
    public String generateQrCode(int amount, String currency) {
        log.debug("Generating mock QR code for amount: {} {}", amount, currency);
        // Generate mock QR URL
        return "https://mock.vnpay.vn/qr/amount-" + amount + "-cur-" + currency + "-" + System.currentTimeMillis();
    }

    @Override
    public boolean verifyPayment(String transactionToken) {
        log.debug("Verifying mock payment: token={}", transactionToken);

        // Simple validation for Sprint 0
        if (transactionToken == null || transactionToken.isBlank()) {
            return false;
        }

        // In mock mode, any token starting with "mock-txn-" is valid
        return transactionToken.startsWith("mock-txn-");
    }

    @Override
    public boolean processRefund(String originalTransactionId, int refundAmount, String reason) {
        log.info("Processing mock refund: transactionId={}, amount={}, reason={}",
                originalTransactionId, refundAmount, reason);

        // In mock mode, always succeed
        return true;
    }
}
