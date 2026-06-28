package com.carebridge.backend.common.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Collections;


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
            log.warn("[DEV-STUB] GeminiTriageClient.analyzeSymptoms called — returning GREEN stub");
            return new GeminiTriageClient.AiTriageResult(RiskLevel.GREEN, "Dev stub — AI guidance only, not a medical diagnosis.");
        };
    }

    @Bean
    public com.carebridge.backend.safety.service.LocationConsentPort safetyLocationConsentPort() {
        return userId -> {
            log.warn("[DEV-STUB] safety.LocationConsentPort.hasLocationConsent({}) — returning false", userId);
            return false;
        };
    }

    @Bean
    public com.carebridge.backend.emergency.service.LocationConsentPort emergencyLocationConsentPort() {
        return userId -> {
            log.warn("[DEV-STUB] emergency.LocationConsentPort.hasLocationConsent({}) — returning false", userId);
            return false;
        };
    }

    @Bean
    public FamilyMemberPort familyMemberPort() {
        return userId -> {
            log.warn("[DEV-STUB] FamilyMemberPort.getFamilyFcmTokens({}) — returning empty list", userId);
            return Collections.emptyList();
        };
    }

    @Bean
    public FcmNotificationPort fcmNotificationPort() {
        return (fcmTokens, payload) ->
            log.warn("[DEV-STUB] FcmNotificationPort.sendBatch({} tokens) — no-op", fcmTokens.size());
    }
}
