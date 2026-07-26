package com.carebridge.backend.triage;

import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.triage.dto.request.ContinueIntakeConversationRequest;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.entity.RedFlagAction;
import com.carebridge.backend.triage.entity.RedFlagSeverity;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.policy.TriageRedFlagPreScreenPolicy;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.RedFlagRuleRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.TriagePreScreenMetrics;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import static com.carebridge.backend.triage.TriagePreScreenTestFactory.MOTHER_ID;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeAiAskMoreEnvelopeJson;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeAiGreenOneShotJson;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeEventSink;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeNeedMoreInfoConversationSession;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeNeutralOneShotRequest;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeOneShotRequest;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeRedEscalateRule;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeRule;
import static com.carebridge.backend.triage.TriagePreScreenTestFactory.makeStartRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-TEST-003 — service-workflow tests (TRFP-TC-011..018).
 * Real TriageRedFlagPreScreenPolicy over a mocked RedFlagRuleRepository; recorded AI stub;
 * list-appending ApplicationEventPublisher (FX-015).
 */
@ExtendWith(MockitoExtension.class)
class TriageServicePreScreenTest {

    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private ChildTriageAiClient childTriageAiClient;
    @Mock private TriageGraphService triageGraphService;
    @Mock private RedFlagRuleRepository redFlagRuleRepository;
    @Mock private TriagePreScreenMetrics preScreenMetrics;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Object> events = makeEventSink();

    private TriageService service() {
        ApplicationEventPublisher recordingPublisher = events::add;
        TriageService service = new TriageService(
                intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, recordingPublisher);
        ReflectionTestUtils.setField(service, "preScreenPolicy",
                new TriageRedFlagPreScreenPolicy(redFlagRuleRepository, preScreenMetrics));
        ReflectionTestUtils.setField(service, "preScreenMetrics", preScreenMetrics);
        return service;
    }

    private AtomicReference<IntakeSession> stubSaveThrough() {
        AtomicReference<IntakeSession> lastSaved = new AtomicReference<>();
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            lastSaved.set(session);
            return session;
        });
        return lastSaved;
    }

    @Test
    void runIntake_redRuleMatch_shortCircuitsWithoutAiAndPersistsCanonicalRedContract() {
        // TRFP-TC-011 (CRITICAL) — oracle: hasCanonicalRedContract, applyCanonicalSnapshot
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));
        AtomicReference<IntakeSession> lastSaved = stubSaveThrough();

        IntakeSessionResponse response = service().runIntake(makeOneShotRequest(), MOTHER_ID);

        verifyNoInteractions(childTriageAiClient);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getRiskLevel()).isEqualTo("RED");
        IntakeSession session = lastSaved.get();
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(session.isEmergency()).isTrue();
        assertThat(session.getCompletedAt()).isNotNull();
        assertThat(session.getRawAiResponse())
                .contains("\"emergencyActionRequired\":true")
                .contains("\"recommendationCode\":\"SEEK_EMERGENCY_CARE\"")
                .contains("RED_FLAG_RULE_PRESCREEN")
                .contains("ngã đập đầu");
    }

    @Test
    void runIntake_shortCircuit_publishesExactlyOneEscalationAndOneCompletionEvent() {
        // TRFP-TC-012 (CRITICAL) — oracle: publishCompletionEvents
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));
        AtomicReference<IntakeSession> lastSaved = stubSaveThrough();

        service().runIntake(makeOneShotRequest(), MOTHER_ID);

        List<EmergencyEscalationTriggered> escalations = events.stream()
                .filter(EmergencyEscalationTriggered.class::isInstance)
                .map(EmergencyEscalationTriggered.class::cast)
                .toList();
        List<IntakeSessionCompleted> completions = events.stream()
                .filter(IntakeSessionCompleted.class::isInstance)
                .map(IntakeSessionCompleted.class::cast)
                .toList();
        assertThat(escalations).hasSize(1);
        assertThat(escalations.getFirst().triggerSource()).isEqualTo("AUTO_TRIAGE");
        assertThat(escalations.getFirst().sessionId()).isEqualTo(lastSaved.get().getId());
        assertThat(escalations.getFirst().userId()).isEqualTo(MOTHER_ID);
        assertThat(completions).hasSize(1);
        assertThat(completions.getFirst().riskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(events).hasSize(2);
    }

    @Test
    void startConversation_redRuleMatch_returnsTriageCompleteEnvelopeAndCompletesSession() {
        // TRFP-TC-013 (CRITICAL) — oracle: CONVERSATION_RESPONSE_FIELDS, persistConversationEnvelope
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));
        when(intakeSessionRepository.findByUserIdAndClientRequestId(eq(MOTHER_ID), anyString()))
                .thenReturn(Optional.empty());
        AtomicReference<IntakeSession> lastSaved = stubSaveThrough();

        IntakeConversationResponse response = service().startConversation(makeStartRequest(), MOTHER_ID);

        verifyNoInteractions(childTriageAiClient);
        assertThat(response.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(response.getTriageResult())
                .containsEntry("riskLevel", "RED")
                .containsEntry("emergencyActionRequired", true)
                .containsEntry("recommendationCode", "SEEK_EMERGENCY_CARE");
        assertThat((List<?>) response.getTriageResult().get("matchedRules")).isNotEmpty();
        IntakeSession session = lastSaved.get();
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(session.isEmergency()).isTrue();
        assertThat(events).anyMatch(EmergencyEscalationTriggered.class::isInstance);
        assertThat(events).anyMatch(IntakeSessionCompleted.class::isInstance);
    }

    @Test
    void continueConversation_newAnswerIntroducesRedKeyword_shortCircuitsOnContinuePath() {
        // TRFP-TC-014 — insertion point after the TRIAGE-010 answer filter
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));
        IntakeSession session = makeNeedMoreInfoConversationSession();
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), MOTHER_ID))
                .thenReturn(Optional.of(session));
        stubSaveThrough();

        IntakeConversationResponse response = service().continueConversation(
                ContinueIntakeConversationRequest.builder()
                        .intakeSessionId(session.getId().toString())
                        .newAnswers(Map.of("parentFreeText", "bé vừa bị nga dap dau"))
                        .build(),
                MOTHER_ID);

        verifyNoInteractions(childTriageAiClient);
        assertThat(response.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(response.getTriageResult())
                .containsEntry("riskLevel", "RED")
                .containsEntry("emergencyActionRequired", true)
                .containsEntry("recommendationCode", "SEEK_EMERGENCY_CARE");
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(session.isEmergency()).isTrue();
        assertThat(events).anyMatch(EmergencyEscalationTriggered.class::isInstance);
        assertThat(events).anyMatch(IntakeSessionCompleted.class::isInstance);
    }

    @Test
    void runIntake_noRuleMatch_callsAiUnchangedPassthrough() {
        // TRFP-TC-015 — regression guard: no behavioral change off-match
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));
        when(childTriageAiClient.triageChild(any())).thenReturn(makeAiGreenOneShotJson());
        AtomicReference<IntakeSession> lastSaved = stubSaveThrough();
        RunIntakeRequest request = makeNeutralOneShotRequest();

        service().runIntake(request, MOTHER_ID);

        verify(childTriageAiClient, times(1)).triageChild(request);
        IntakeSession session = lastSaved.get();
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.GREEN);
        assertThat(session.getRawAiResponse()).doesNotContain("RED_FLAG_RULE_PRESCREEN");
        assertThat(events).noneMatch(EmergencyEscalationTriggered.class::isInstance);
    }

    @Test
    void runIntake_degradedPreScreenAndAiFailure_hardcodedFallbackChainStillRuns() {
        // TRFP-TC-016 (CRITICAL) — ADR-003 / BR-SAFETY-TRFP-002/003 defense-in-depth
        when(redFlagRuleRepository.findByActiveTrue())
                .thenThrow(new DataAccessResourceFailureException("db down")); // FX-007
        when(childTriageAiClient.triageChild(any())).thenThrow(new RuntimeException("python down"));
        when(triageGraphService.run(any())).thenReturn(hardcodedRedResult());
        AtomicReference<IntakeSession> lastSaved = stubSaveThrough();
        RunIntakeRequest request = makeOneShotRequest(r -> r.setBreathingStatus("khó thở, rút lõm"));

        assertDoesNotThrow(() -> service().runIntake(request, MOTHER_ID));

        IntakeSession session = lastSaved.get();
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(session.getRawAiResponse())
                .contains("RED_BREATHING_DISTRESS")
                .doesNotContain("RED_FLAG_RULE_PRESCREEN");
        assertThat(events.stream().filter(EmergencyEscalationTriggered.class::isInstance)).hasSize(1);
        verify(preScreenMetrics, times(1)).recordDegraded(anyString());
    }

    @Test
    void startConversation_yellowMatch_annotatesAndContinuesToAiWithoutShortCircuit() {
        // TRFP-TC-017 — ADR-002 annotation path; O1 resolved positive (pydantic ignores extras)
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRule(r -> {
            r.setKeyword("sốt kéo dài");
            r.setSeverity(RedFlagSeverity.YELLOW);
            r.setAction(RedFlagAction.WARN);
        }))); // FX-003
        when(intakeSessionRepository.findByUserIdAndClientRequestId(eq(MOTHER_ID), anyString()))
                .thenReturn(Optional.empty());
        when(childTriageAiClient.startIntake(any())).thenReturn(makeAiAskMoreEnvelopeJson());
        AtomicReference<IntakeSession> lastSaved = stubSaveThrough();

        IntakeConversationResponse response = service().startConversation(
                makeStartRequest(r -> r.setInitialText("bé sốt kéo dài ba ngày")), MOTHER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
        verify(childTriageAiClient, times(1)).startIntake(requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .containsEntry("preScreenFlags", List.of("sốt kéo dài"));
        assertThat(response.getStatus()).isEqualTo("ASK_MORE");
        assertThat(response.getTriageResult()).isNull();
        IntakeSession session = lastSaved.get();
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.NEED_MORE_INFO);
        assertThat(session.getRiskLevel()).isNull();
        assertThat(events).noneMatch(EmergencyEscalationTriggered.class::isInstance);
        verify(preScreenMetrics, times(1)).recordAnnotation("conversation_start");
    }

    @Test
    void startConversation_idempotentReplayAfterShortCircuit_returnsStoredEnvelopeWithoutDuplicateEvents() {
        // TRFP-TC-018 — replay contract (:255-261) + exactly-once escalation
        when(redFlagRuleRepository.findByActiveTrue()).thenReturn(List.of(makeRedEscalateRule()));
        AtomicReference<IntakeSession> stored = new AtomicReference<>();
        when(intakeSessionRepository.findByUserIdAndClientRequestId(eq(MOTHER_ID), anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            stored.set(session);
            return session;
        });
        TriageService service = service();

        IntakeConversationResponse first = service.startConversation(makeStartRequest(), MOTHER_ID);
        int eventCountAfterFirst = events.size();

        IntakeConversationResponse replay = service.startConversation(makeStartRequest(), MOTHER_ID);

        verifyNoInteractions(childTriageAiClient);
        assertThat(first.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(replay.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(replay.getTriageResult()).containsEntry("riskLevel", "RED");
        assertThat(events).hasSize(eventCountAfterFirst);
        assertThat(events.stream().filter(EmergencyEscalationTriggered.class::isInstance)).hasSize(1);
        assertThat(events.stream().filter(IntakeSessionCompleted.class::isInstance)).hasSize(1);
    }

    private ChildTriageResult hardcodedRedResult() {
        return ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel("RED")
                .riskColor("#EF4444")
                .summary("Immediate danger sign")
                .recommendedAction("Seek emergency care")
                .emergencyActionRequired(true)
                .redFlags(List.of("Difficulty breathing"))
                .matchedRules(List.of("RED_BREATHING_DISTRESS"))
                .citations(List.of())
                .disclaimer("Risk classification only.")
                .questions(List.of())
                .build();
    }
}
