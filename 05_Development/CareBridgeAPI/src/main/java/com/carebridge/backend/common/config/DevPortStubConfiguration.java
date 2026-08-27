package com.carebridge.backend.common.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Dev-only extraction stub. Triage outcomes are never produced by a Java Gemini port. */
@Configuration
@Profile("dev & !test")
public class DevPortStubConfiguration {
    private static final Logger log = LoggerFactory.getLogger(DevPortStubConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(GeminiExtractionClient.class)
    public GeminiExtractionClient geminiExtractionClient() {
        return constrainedPrompt -> {
            log.warn("[DEV-STUB] GeminiExtractionClient.extractStructuredData called - returning stub result");
            return new GeminiExtractionClient.ExtractionResult("[]", 1, "MILD", false);
        };
    }
}
