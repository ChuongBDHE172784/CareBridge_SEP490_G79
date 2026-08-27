package com.carebridge.backend.common.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Fallback extraction port for local startup; it cannot choose a triage outcome. */
@Configuration
public class DevPortMockConfiguration {
    @Bean
    @ConditionalOnMissingBean(GeminiExtractionClient.class)
    public GeminiExtractionClient geminiExtractionClient() {
        return prompt -> new GeminiExtractionClient.ExtractionResult("[]", 1, "LOW", false);
    }
}
