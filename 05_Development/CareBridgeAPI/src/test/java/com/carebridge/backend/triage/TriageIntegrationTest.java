package com.carebridge.backend.triage;

import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.ITriageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class TriageIntegrationTest {

    @Autowired private ITriageService triageService;
    @Autowired private IIntakeSessionRepository intakeSessionRepository;
    @MockitoBean private GeminiTriageClient geminiTriageClient;
    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    @Test
    void runIntake_fullFlow_shouldPersistToDb() {
        // TRIAGE-TC-INT-001
        when(geminiTriageClient.analyzeSymptoms(anyString()))
                .thenReturn(new GeminiTriageClient.AiTriageResult(RiskLevel.GREEN, "AI guidance only."));

        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        var response = triageService.runIntake(TriageTestFactory.makeRunIntakeRequest(), userId);

        assertThat(response.getSessionId()).isNotNull();

        IntakeSession record = intakeSessionRepository.findById(response.getSessionId()).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(record.getRiskLevel()).isEqualTo(RiskLevel.GREEN);
        assertThat(record.getDisclaimer()).isNotBlank();
    }
}
