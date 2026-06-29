package com.carebridge.backend.triage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.GeminiTriageClient;
import com.carebridge.backend.triage.service.impl.TriageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {

    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private GeminiTriageClient geminiTriageClient;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private TriageService triageService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void runIntake_validSymptoms_shouldReturnCompletedSession() {
        // TRIAGE-TC-001
        GeminiTriageClient.AiTriageResult aiResult = new GeminiTriageClient.AiTriageResult(
                RiskLevel.GREEN, "AI provides guidance only, not medical diagnosis.");
        when(geminiTriageClient.analyzeSymptoms(anyString())).thenReturn(aiResult);

        IntakeSession saved = TriageTestFactory.makeIntakeSession(s -> {
            s.setStatus(IntakeStatus.COMPLETED);
            s.setRiskLevel(RiskLevel.GREEN);
            s.setDisclaimer("AI provides guidance only, not medical diagnosis.");
        });
        when(intakeSessionRepository.save(any())).thenReturn(saved);

        IntakeSessionResponse result = triageService.runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(result.getSessionId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getDisclaimer()).isNotBlank();
    }

    @Test
    void runIntake_geminiTimeout_shouldSaveFailedStatus() {
        // TRIAGE-TC-004
        when(geminiTriageClient.analyzeSymptoms(anyString()))
                .thenThrow(new RuntimeException("Gemini timeout"));

        IntakeSession failedSession = TriageTestFactory.makeIntakeSession(s -> s.setStatus(IntakeStatus.FAILED));
        when(intakeSessionRepository.save(any())).thenReturn(failedSession);

        assertThatThrownBy(() -> triageService.runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID))
                .isInstanceOf(Exception.class);

        verify(intakeSessionRepository, atLeastOnce()).save(any(IntakeSession.class));
    }

    @Test
    void runIntake_shouldNotLogSymptomText() {
        // TRIAGE-TC-007 — PDPA compliance
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Logger logger = (Logger) LoggerFactory.getLogger(TriageService.class);
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            GeminiTriageClient.AiTriageResult aiResult = new GeminiTriageClient.AiTriageResult(
                    RiskLevel.GREEN, "AI guidance only.");
            when(geminiTriageClient.analyzeSymptoms(anyString())).thenReturn(aiResult);
            IntakeSession saved = TriageTestFactory.makeIntakeSession(s -> {
                s.setStatus(IntakeStatus.COMPLETED);
                s.setRiskLevel(RiskLevel.GREEN);
                s.setDisclaimer("AI guidance only.");
            });
            when(intakeSessionRepository.save(any())).thenReturn(saved);

            triageService.runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

            boolean symptomInLog = listAppender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("SYNTHETIC_SYMPTOMS_TEST_DATA"));
            assertThat(symptomInLog).as("Symptom text must NOT appear in logs (PDPA)").isFalse();
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}
