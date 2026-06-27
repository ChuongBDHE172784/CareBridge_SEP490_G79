package com.carebridge.backend.config;

import com.carebridge.backend.ai.service.GeminiExtractionClient;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides mock implementations for port interfaces that have no production adapters yet.
 * Auto-detected by @SpringBootTest via test-classpath component scan.
 * Individual tests may override specific beans with @MockitoBean for configured behavior.
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

    @Bean
    public com.carebridge.backend.safety.service.LocationConsentPort safetyLocationConsentPort() {
        return Mockito.mock(com.carebridge.backend.safety.service.LocationConsentPort.class);
    }

    @Bean
    public com.carebridge.backend.emergency.service.LocationConsentPort emergencyLocationConsentPort() {
        return Mockito.mock(com.carebridge.backend.emergency.service.LocationConsentPort.class);
    }

    @Bean
    public FamilyMemberPort familyMemberPort() {
        return Mockito.mock(FamilyMemberPort.class);
    }

    @Bean
    public FcmNotificationPort fcmNotificationPort() {
        return Mockito.mock(FcmNotificationPort.class);
    }
}
