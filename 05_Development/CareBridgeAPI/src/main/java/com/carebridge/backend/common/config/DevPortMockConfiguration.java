package com.carebridge.backend.common.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.triage.exception.AiOutcomeUnavailableException;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.RiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Fallback / mock configuration for port interfaces that do not have
 * active production adapters yet, allowing the app to run in local/dev.
 */
@Configuration
public class DevPortMockConfiguration {

    @Bean
    @ConditionalOnMissingBean(GeminiExtractionClient.class)
    public GeminiExtractionClient geminiExtractionClient() {
        return prompt -> new GeminiExtractionClient.ExtractionResult(
                "[]", 1, "LOW", false
        );
    }

    @Bean
    @ConditionalOnMissingBean(GeminiTriageClient.class)
    public GeminiTriageClient geminiTriageClient() {
        // Used to return GREEN. A mock must never be able to produce a triage colour —
        // an absent integration is "cannot determine", not "low risk".
        return prompt -> {
            throw new AiOutcomeUnavailableException(
                    "Mock GeminiTriageClient cannot produce a triage outcome");
        };
    }

    // Emergency LocationConsentPort, FamilyMemberPort, and
    // FcmNotificationPort now have real @Component adapters
    // (consent/family/notification modules) — their mock fallbacks here were
    // removed to avoid duplicate-bean conflicts with those adapters.
}
