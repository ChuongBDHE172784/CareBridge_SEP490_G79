package com.carebridge.backend.common.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.RiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;


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
        return prompt -> new GeminiTriageClient.AiTriageResult(
                RiskLevel.GREEN, "Mock disclaimer: Always consult a professional."
        );
    }

    @Bean
    @ConditionalOnMissingBean(com.carebridge.backend.safety.service.LocationConsentPort.class)
    public com.carebridge.backend.safety.service.LocationConsentPort safetyLocationConsentPort() {
        return userId -> true;
    }

    @Bean
    @ConditionalOnMissingBean(com.carebridge.backend.emergency.service.LocationConsentPort.class)
    public com.carebridge.backend.emergency.service.LocationConsentPort emergencyLocationConsentPort() {
        return userId -> true;
    }

    @Bean
    @ConditionalOnMissingBean(com.carebridge.backend.emergency.service.FamilyMemberPort.class)
    public com.carebridge.backend.emergency.service.FamilyMemberPort familyMemberPort() {
        return userId -> Collections.emptyList();
    }

    @Bean
    @ConditionalOnMissingBean(com.carebridge.backend.emergency.service.FcmNotificationPort.class)
    public com.carebridge.backend.emergency.service.FcmNotificationPort fcmNotificationPort() {
        return (tokens, payload) -> {};
    }
}
