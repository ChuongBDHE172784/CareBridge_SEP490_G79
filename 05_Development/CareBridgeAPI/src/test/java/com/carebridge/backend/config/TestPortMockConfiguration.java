package com.carebridge.backend.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides mock implementations for port interfaces that have no production adapters yet.
 * Auto-detected by @SpringBootTest via test-classpath component scan.
 * Individual tests may override specific beans with @MockitoBean for configured behavior.
 *
 * LocationConsentPort (safety + emergency), FamilyMemberPort, and FcmNotificationPort
 * now have real @Component adapters, so their mocks here were removed to avoid
 * duplicate-bean conflicts — tests needing specific behavior for those ports
 * should @MockitoBean-override the real adapter bean instead.
 */
@Configuration
public class TestPortMockConfiguration {

    @Bean
    public GeminiExtractionClient geminiExtractionClient() {
        return Mockito.mock(GeminiExtractionClient.class);
    }

    @Bean
    public GeminiTriageClient geminiTriageClient() {
        return Mockito.mock(GeminiTriageClient.class);
    }
}
