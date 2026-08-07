package com.carebridge.backend.common.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.exception.AiOutcomeUnavailableException;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


/**
 * Dev-only stubs for port interfaces that have no real adapter yet.
 * Activated by spring.profiles.active=dev (set in application.yaml).
 */
@Configuration
@Profile("dev")
public class DevPortStubConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DevPortStubConfiguration.class);

    @Bean
    public GeminiExtractionClient geminiExtractionClient() {
        return constrainedPrompt -> {
            log.warn("[DEV-STUB] GeminiExtractionClient.extractStructuredData called — returning stub result");
            return new GeminiExtractionClient.ExtractionResult("[]", 1, "MILD", false);
        };
    }

    @Bean
    public GeminiTriageClient geminiTriageClient() {
        return constrainedPrompt -> {
            // Used to return GREEN, which made a missing integration look like a reassuring
            // triage result. A stub must never be able to produce a colour.
            log.warn("[DEV-STUB] GeminiTriageClient.analyzeSymptoms called — failing closed");
            throw new AiOutcomeUnavailableException(
                    "Dev stub cannot produce a triage outcome; callers must degrade to NEEDS_MORE_INFO");
        };
    }

    // Emergency LocationConsentPort, FamilyMemberPort, and
    // FcmNotificationPort now have real @Component adapters
    // (consent/family/notification modules) — their dev stubs here were
    // removed to avoid duplicate-bean conflicts with those adapters.
}
