package com.carebridge.backend.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Test extraction port. Canonical triage disposition is deterministic and has no Gemini port. */
@Configuration
public class TestPortMockConfiguration {
    @Bean
    public GeminiExtractionClient geminiExtractionClient() {
        return Mockito.mock(GeminiExtractionClient.class);
    }
}
