package com.carebridge.backend.triage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {

    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private ChildTriageAiClient childTriageAiClient;
    @Mock private TriageGraphService triageGraphService;
    @Mock private ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private TriageService service() {
        return new TriageService(intakeSessionRepository, childTriageAiClient, triageGraphService, objectMapper, eventPublisher);
    }

    @Test
    void runIntake_validSymptoms_shouldReturnCompletedSession() {
        // TRIAGE-TC-001
        when(childTriageAiClient.triageChild(any())).thenReturn(greenJson());

        IntakeSession saved = TriageTestFactory.makeIntakeSession(s -> {
            s.setStatus(IntakeStatus.COMPLETED);
            s.setRiskLevel(RiskLevel.GREEN);
            s.setDisclaimer("AI provides guidance only, not medical diagnosis.");
        });
        when(intakeSessionRepository.save(any())).thenReturn(saved);

        IntakeSessionResponse result = service().runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(result.getSessionId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getDisclaimer()).isNotBlank();
        verify(triageGraphService, never()).run(any());
    }

    @Test
    void runIntake_aiServiceUnavailable_shouldFallbackToJavaRules() {
        // TRIAGE-TC-004
        when(childTriageAiClient.triageChild(any()))
                .thenThrow(new RuntimeException("AI service unavailable"));
        when(triageGraphService.run(any())).thenReturn(greenResult());

        IntakeSession saved = TriageTestFactory.makeIntakeSession(s -> {
            s.setStatus(IntakeStatus.COMPLETED);
            s.setRiskLevel(RiskLevel.GREEN);
            s.setDisclaimer("AI guidance only.");
        });
        when(intakeSessionRepository.save(any())).thenReturn(saved);

        IntakeSessionResponse result = service().runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRiskLevel()).isEqualTo("GREEN");
        verify(triageGraphService).run(any());
    }

    @Test
    void runIntake_aiAndFallbackFailure_shouldSaveFailedStatus() {
        // TRIAGE-TC-004
        when(childTriageAiClient.triageChild(any()))
                .thenThrow(new RuntimeException("AI service unavailable"));
        when(triageGraphService.run(any()))
                .thenThrow(new RuntimeException("Fallback triage failed"));

        IntakeSession failedSession = TriageTestFactory.makeIntakeSession(s -> s.setStatus(IntakeStatus.FAILED));
        when(intakeSessionRepository.save(any())).thenReturn(failedSession);

        assertThatThrownBy(() -> service().runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID))
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
            when(childTriageAiClient.triageChild(any())).thenReturn(greenJson());
            IntakeSession saved = TriageTestFactory.makeIntakeSession(s -> {
                s.setStatus(IntakeStatus.COMPLETED);
                s.setRiskLevel(RiskLevel.GREEN);
                s.setDisclaimer("AI guidance only.");
            });
            when(intakeSessionRepository.save(any())).thenReturn(saved);

            service().runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

            boolean symptomInLog = listAppender.list.stream()
                    .anyMatch(e -> e.getFormattedMessage().contains("SYNTHETIC_SYMPTOMS_TEST_DATA"));
            assertThat(symptomInLog).as("Symptom text must NOT appear in logs (PDPA)").isFalse();
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    private ChildTriageResult greenResult() {
        return ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel("GREEN")
                .riskColor("#22C55E")
                .summary("No red flags")
                .recommendedAction("Monitor at home")
                .emergencyActionRequired(false)
                .redFlags(java.util.List.of())
                .matchedRules(java.util.List.of("GREEN_MILD_NO_RED_FLAGS"))
                .citations(java.util.List.of())
                .disclaimer("AI guidance only.")
                .questions(java.util.List.of())
                .build();
    }

    private String greenJson() {
        return """
                {
                  "riskLevel": "GREEN",
                  "riskColor": "#22C55E",
                  "summary": "No red flags",
                  "possibleConcern": "Mild symptoms",
                  "recommendedAction": "Monitor at home",
                  "emergencyActionRequired": false,
                  "redFlags": [],
                  "matchedRules": ["GREEN_MILD_NO_RED_FLAGS"],
                  "citations": [],
                  "questions": [],
                  "warning": "Không tìm thấy nguồn phù hợp trong knowledge base.",
                  "disclaimer": "AI guidance only."
                }
                """;
    }
}
