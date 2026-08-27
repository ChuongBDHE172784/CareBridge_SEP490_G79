package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * GEMINI_ENABLED=true with a missing API key must still boot the application context and
 * report an explicit NOT_CONFIGURED state (never a crash, never silent SAFE behavior).
 * The enabled=false path is covered by the default-context tests (BackendApplicationTests).
 */
@SpringBootTest(properties = {
        "carebridge.gemini.enabled=true",
        "carebridge.gemini.api-key="
})
class AiModerationConfigSmokeTest {

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Autowired
    private GeminiModerationClient geminiModerationClient;

    @Test
    void contextStarts_withEnabledButMissingKey_reportsNotConfigured() {
        assertThat(geminiModerationClient.configState())
                .isEqualTo(GeminiModerationClient.ConfigState.NOT_CONFIGURED);
    }
}
