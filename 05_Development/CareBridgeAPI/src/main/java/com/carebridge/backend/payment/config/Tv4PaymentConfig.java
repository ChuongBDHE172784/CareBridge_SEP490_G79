package com.carebridge.backend.payment.config;

import com.carebridge.backend.payment.service.MockPaymentService;
import com.carebridge.backend.payment.service.PaymentService;
import com.carebridge.backend.payment.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * TV4 Payment Configuration.
 *
 * Sprint 0: Registers mock providers.
 * Sprint 1+: Replace with actual provider integrations.
 */
@Configuration
@RequiredArgsConstructor
public class Tv4PaymentConfig {

    @Value("${payment.mock.enabled:true}")
    private boolean paymentMockEnabled;

    @Value("${realtime.provider:mock}")
    private String realtimeProvider;

    /**
     * Payment service (mock or real based on config).
     */
    @Bean
    @Primary
    public PaymentService paymentService() {
        if (paymentMockEnabled) {
            return new MockPaymentServiceWithFallback();
        }
        // Sprint 1+: return new VNPayService(...);
        throw new IllegalStateException("Real payment provider not configured");
    }

    /**
     * Realtime service.
     */
    @Bean
    @Primary
    public RealtimeService realtimeService() {
        if ("mock".equalsIgnoreCase(realtimeProvider)) {
            return new RealtimeService();
        }
        // Sprint 1+: return new ZegoCloudService(...);
        throw new IllegalStateException("Realtime provider not configured");
    }

    /**
     * Extended mock payment service with repository access.
     */
    static class MockPaymentServiceWithFallback extends MockPaymentService {
        // Inherits all mock functionality
    }
}
