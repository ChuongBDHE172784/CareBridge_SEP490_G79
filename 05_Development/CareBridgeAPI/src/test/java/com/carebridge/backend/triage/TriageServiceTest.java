package com.carebridge.backend.triage;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
import com.carebridge.backend.triage.dto.response.TriageResultResponse;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.request.ContinueIntakeConversationRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.TriageFallbackMetrics;
import com.carebridge.backend.triage.service.TriageStageLegacyDefaultMetrics;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.net.http.HttpTimeoutException;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {

    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private ChildTriageAiClient childTriageAiClient;
    @Mock private TriageGraphService triageGraphService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private LifecycleConsentValidator lifecycleConsentValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private TriageService service() {
        return new TriageService(intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, eventPublisher, lifecycleConsentValidator);
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
    void runIntake_normalPythonResponse_shouldNotFallbackWithinConfiguredSpringBudget() {
        TriageFallbackMetrics metrics = new TriageFallbackMetrics();
        when(childTriageAiClient.triageChild(any())).thenReturn(greenJson());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeSessionResponse result = new TriageService(
                intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, eventPublisher, metrics)
                .runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(metrics.totalCount()).isZero();
        verify(triageGraphService, never()).run(any());
    }

    @Test
    void runIntake_pythonTimeout_shouldRecordTimeoutFallbackSeparately() {
        TriageFallbackMetrics metrics = new TriageFallbackMetrics();
        when(childTriageAiClient.triageChild(any()))
                .thenThrow(new IllegalStateException("AI triage service unavailable", new HttpTimeoutException("timed out")));
        when(triageGraphService.run(any())).thenReturn(greenResult());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new TriageService(intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, eventPublisher, metrics)
                .runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(metrics.totalCount()).isEqualTo(1);
        assertThat(metrics.count(TriageFallbackMetrics.Reason.TIMEOUT)).isEqualTo(1);
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

    @Test
    void startConversation_shouldUseAndPersistCanonicalDatabaseSessionId() {
        IntakeSession session = TriageTestFactory.makeIntakeSession(s -> {
            s.setStatus(IntakeStatus.PROCESSING);
            s.setRiskLevel(null);
        });
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","intakeSessionId":"client-id","mergedIntake":{},"questions":[{"questionKey":"childAgeMonths","text":"Bé hiện bao nhiêu tháng tuổi?","answerType":"NUMBER","options":[]}],"round":2}
                """);

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("be sot").build(), USER_ID);

        assertThat(response.getIntakeSessionId()).isEqualTo(session.getId().toString());
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.NEED_MORE_INFO);
        assertThat(session.getCompletedAt()).isNull();
        assertThat(session.getRawAiResponse()).contains(session.getId().toString());
    }

    @Test
    void ownershipMismatch_shouldNotExposeSessionThroughGetOrContinue() {
        UUID sessionId = UUID.randomUUID();
        when(intakeSessionRepository.findForUpdateByIdAndUserId(sessionId, USER_ID)).thenReturn(Optional.empty());
        when(intakeSessionRepository.findByIdAndUserId(sessionId, USER_ID)).thenReturn(Optional.empty());

        TriageException continueException = catchThrowableOfType(() -> service().continueConversation(
                ContinueIntakeConversationRequest.builder()
                        .intakeSessionId(sessionId.toString())
                        .newAnswers(Map.of("childAgeMonths", 8))
                        .build(), USER_ID), TriageException.class);
        TriageException getException = catchThrowableOfType(
                () -> service().getResult(sessionId, USER_ID), TriageException.class);

        assertThat(continueException).isNotNull();
        assertThat(continueException.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(continueException.getCode()).isEqualTo("TRIAGE-003");
        assertThat(getException).isNotNull();
        assertThat(getException.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getException.getCode()).isEqualTo("TRIAGE-003");
        verify(childTriageAiClient, never()).continueIntake(any());
        verify(childTriageAiClient, never()).startIntake(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void startConversation_missingStageAndProfiles_shouldDefaultInfantAndRecordLegacyMetric() {
        TriageStageLegacyDefaultMetrics stageMetrics = new TriageStageLegacyDefaultMetrics();
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","assistantMessage":"Need age","mergedIntake":{},"questions":[{"questionKey":"childAgeMonths","text":"Age?","answerType":"NUMBER","options":[]}],"round":1}
                """);

        IntakeConversationResponse response = new TriageService(intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, eventPublisher, new TriageFallbackMetrics(), stageMetrics)
                .startConversation(StartIntakeConversationRequest.builder().initialText("be sot").build(), USER_ID);

        assertThat(response.getStage()).isEqualTo(TriageStage.INFANT.name());
        assertThat(stageMetrics.legacyDefaultTotal()).isEqualTo(1);
    }

    @Test
    void startConversation_maternalProfileWithoutStage_shouldInferPregnancyWithoutLegacyMetric() {
        TriageStageLegacyDefaultMetrics stageMetrics = new TriageStageLegacyDefaultMetrics();
        UUID motherProfileId = UUID.randomUUID();
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","assistantMessage":"Need age","mergedIntake":{},"questions":[{"questionKey":"childAgeMonths","text":"Age?","answerType":"NUMBER","options":[]}],"round":1}
                """);

        IntakeConversationResponse response = new TriageService(intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, eventPublisher, new TriageFallbackMetrics(), stageMetrics)
                .startConversation(StartIntakeConversationRequest.builder()
                        .initialText("toi can tu van")
                        .motherProfileId(motherProfileId)
                        .build(), USER_ID);

        assertThat(response.getStage()).isEqualTo(TriageStage.PREGNANCY.name());
        assertThat(stageMetrics.legacyDefaultTotal()).isZero();
    }

    @Test
    void startConversation_explicitPostpartumStage_shouldPersistAndReturnPostpartumContract() {
        UUID motherProfileId = UUID.randomUUID();
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","assistantMessage":"Need recovery details","mergedIntake":{},
                 "questions":[{"questionKey":"duration","text":"How long?","answerType":"TEXT","options":[]}],"round":1}
                """);

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("toi can ho tro sau sinh")
                        .stage(TriageStage.POSTPARTUM)
                        .motherProfileId(motherProfileId)
                        .currentIntake(Map.of("stage", "POSTPARTUM"))
                        .build(),
                USER_ID);

        ArgumentCaptor<IntakeSession> sessionCaptor = ArgumentCaptor.forClass(IntakeSession.class);
        verify(intakeSessionRepository, times(2)).save(sessionCaptor.capture());
        IntakeSession persisted = sessionCaptor.getAllValues().getLast();
        assertThat(persisted.getStage()).isEqualTo(TriageStage.POSTPARTUM);
        assertThat(persisted.getMotherProfileId()).isEqualTo(motherProfileId);
        assertThat(persisted.getBabyProfileId()).isNull();
        assertThat(response.getStage()).isEqualTo("POSTPARTUM");
        assertThat(response.getMergedIntake()).containsEntry("stage", "POSTPARTUM");
        verify(lifecycleConsentValidator).ensureEligibleForMutation(USER_ID);
        verify(childTriageAiClient, times(1)).startIntake(any());
    }

    @Test
    void startConversation_postpartumMissingConsent_shouldFailBeforePersistenceOrAi() {
        doThrow(consentError("LIFECYCLE_CONSENT_REQUIRED"))
                .when(lifecycleConsentValidator).ensureEligibleForMutation(USER_ID);

        assertThatThrownBy(() -> service().startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("toi can ho tro sau sinh")
                        .stage(TriageStage.POSTPARTUM)
                        .currentIntake(Map.of("stage", "POSTPARTUM"))
                        .build(),
                USER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("LIFECYCLE_CONSENT_REQUIRED"));

        verifyNoInteractions(intakeSessionRepository, childTriageAiClient, triageGraphService, eventPublisher);
    }

    @Test
    void continueConversation_postpartumRevokedConsent_shouldFailBeforeProcessing() {
        IntakeSession session = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","mergedIntake":{"stage":"POSTPARTUM"},
                 "questions":[{"questionKey":"duration","text":"Bao lâu?","answerType":"TEXT","options":[]}],"round":1}
                """);
        session.setStage(TriageStage.POSTPARTUM);
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID))
                .thenReturn(Optional.of(session));
        doThrow(consentError("LIFECYCLE_CONSENT_INVALID"))
                .when(lifecycleConsentValidator).ensureEligibleForMutation(USER_ID);

        assertThatThrownBy(() -> service().continueConversation(
                ContinueIntakeConversationRequest.builder()
                        .intakeSessionId(session.getId().toString())
                        .newAnswers(Map.of("duration", "1 ngay"))
                        .build(),
                USER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("LIFECYCLE_CONSENT_INVALID"));

        verify(childTriageAiClient, never()).continueIntake(any());
        verify(intakeSessionRepository, never()).save(any());
        verifyNoInteractions(triageGraphService, eventPublisher);
    }

    @Test
    void runIntake_postpartumExpiredConsent_shouldFailBeforePersistenceOrAi() {
        var request = TriageTestFactory.makeRunIntakeRequest();
        request.setStage(TriageStage.POSTPARTUM);
        request.setBabyProfileId(null);
        doThrow(consentError("LIFECYCLE_CONSENT_INVALID"))
                .when(lifecycleConsentValidator).ensureEligibleForMutation(USER_ID);

        assertThatThrownBy(() -> service().runIntake(request, USER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("LIFECYCLE_CONSENT_INVALID"));

        verifyNoInteractions(intakeSessionRepository, childTriageAiClient, triageGraphService, eventPublisher);
    }

    @Test
    void startConversation_postpartumWithBabyProfile_shouldRejectBeforeAi() {
        StartIntakeConversationRequest request = StartIntakeConversationRequest.builder()
                .initialText("toi can ho tro sau sinh")
                .stage(TriageStage.POSTPARTUM)
                .babyProfileId(UUID.randomUUID())
                .currentIntake(Map.of("stage", "POSTPARTUM"))
                .build();

        assertThatThrownBy(() -> service().startConversation(request, USER_ID))
                .isInstanceOfSatisfying(TriageException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TRIAGE-012"));
        verifyNoInteractions(childTriageAiClient, eventPublisher);
        verify(intakeSessionRepository, never()).save(any());
    }

    @Test
    void startConversation_postpartumInfantQuestion_shouldUseLocalFallbackWithoutSecondAi() {
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","mergedIntake":{"stage":"POSTPARTUM","childAgeMonths":2},
                 "questions":[{"questionKey":"childAgeMonths","text":"Baby age?","answerType":"NUMBER","options":[]}],
                 "round":1}
                """);
        when(triageGraphService.run(any())).thenReturn(ChildTriageResult.builder()
                .status("NEED_MORE_INFO")
                .matchedRules(java.util.List.of("POSTPARTUM_RULES_REQUIRE_CLINICAL_REVIEW"))
                .questions(java.util.List.of("Dấu hiệu đã xuất hiện bao lâu?"))
                .citations(java.util.List.of())
                .disclaimer("Không thay thế nhân viên y tế.")
                .build());

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("toi thay chong mat")
                        .stage(TriageStage.POSTPARTUM)
                        .currentIntake(Map.of("stage", "POSTPARTUM"))
                        .build(),
                USER_ID);

        assertThat(response.getStage()).isEqualTo("POSTPARTUM");
        java.util.List<String> questionKeys = response.getQuestions().stream()
                .map(question -> String.valueOf(((Map<?, ?>) question).get("questionKey")))
                .toList();
        assertThat(questionKeys)
                .doesNotContain("childAgeMonths", "feedingStatus");
        assertThat(response.getMergedIntake())
                .doesNotContainKeys("babyProfileId", "childAgeMonths", "feedingStatus");
        verify(childTriageAiClient, times(1)).startIntake(any());
        verify(childTriageAiClient, never()).continueIntake(any());
        verify(triageGraphService, times(1)).run(any());
    }

    @Test
    void startConversation_duplicateClientRequestId_shouldReplayOriginalSessionWithoutSideEffects() {
        String clientRequestId = "idempotency-key-1234";
        IntakeSession existing = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","intakeSessionId":"00000000-0000-0000-0000-000000000001",
                 "mergedIntake":{},"questions":[{"questionKey":"childAgeMonths","text":"Age?","answerType":"NUMBER","options":[]}],"round":1}
                """);
        existing.setClientRequestId(clientRequestId);
        when(intakeSessionRepository.findByUserIdAndClientRequestId(USER_ID, clientRequestId))
                .thenReturn(Optional.of(existing));

        IntakeConversationResponse response = service().startConversation(StartIntakeConversationRequest.builder()
                .initialText("be sot")
                .clientRequestId(clientRequestId)
                .build(), USER_ID);

        assertThat(response.getIntakeSessionId()).isEqualTo(existing.getId().toString());
        assertThat(response.getStatus()).isEqualTo("ASK_MORE");
        verify(intakeSessionRepository, never()).save(any());
        verifyNoInteractions(childTriageAiClient, eventPublisher);
    }

    @Test
    void continueConversation_clientStateCannotOverridePersistedCanonicalState() {
        IntakeSession session = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","intakeSessionId":"00000000-0000-0000-0000-000000000001",
                 "mergedIntake":{"childAgeMonths":8,"breathingStatus":"tho binh thuong"},"questions":[{"questionKey":"duration","text":"Triệu chứng kéo dài bao lâu?","answerType":"TEXT","options":[]}],"round":2}
                """);
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(childTriageAiClient.continueIntake(any())).thenReturn("""
                {"status":"ASK_MORE","mergedIntake":{"childAgeMonths":8},"questions":[{"questionKey":"duration","text":"Triệu chứng kéo dài bao lâu?","answerType":"TEXT","options":[]}],"round":3}
                """);

        service().continueConversation(ContinueIntakeConversationRequest.builder()
                .intakeSessionId(session.getId().toString())
                .currentIntake(Map.of("childAgeMonths", 99))
                .round(99)
                .newAnswers(Map.of("duration", "1 ngay"))
                .build(), USER_ID);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(childTriageAiClient).continueIntake(captor.capture());
        assertThat(captor.getValue().get("round")).isEqualTo(2);
        assertThat(((Map<?, ?>) captor.getValue().get("currentIntake")).get("childAgeMonths")).isEqualTo(8);
    }

    @Test
    void pythonRedConversation_shouldPersistCanonicalRedContract() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"TRIAGE_COMPLETE","mergedIntake":{"breathingStatus":"kho tho"},"questions":[],"round":1,
                 "triageResult":{"riskLevel":"RED","emergencyActionRequired":true,"matchedRules":["RED_BREATHING_DISTRESS"],"recommendationCode":"SEEK_EMERGENCY_CARE",
                 "graphVersion":"python-1","ruleSetVersion":"rules-1","ontologyVersion":"ontology-1","responseSchemaVersion":"2.0"}}
                """);

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("be kho tho").build(), USER_ID);

        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(response.getTriageResult()).containsEntry("riskLevel", "RED")
                .containsEntry("emergencyActionRequired", true);
        verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
    }

    @Test
    void javaFallbackRedConversation_shouldShareVersionedRedContract() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenReturn(redResult());

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("be kho tho").build(), USER_ID);

        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(response.getTriageResult()).containsEntry("riskLevel", "RED")
                .containsEntry("emergencyActionRequired", true)
                .containsKeys("graphVersion", "ruleSetVersion", "ontologyVersion", "responseSchemaVersion");
    }

    @Test
    void javaFallbackAskMore_shouldReturnStructuredVietnameseQuestions() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenReturn(ChildTriageResult.builder()
                .status("NEED_MORE_INFO")
                .questions(java.util.List.of("Trẻ hiện bao nhiêu tháng tuổi?"))
                .build());

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("bé ho và sốt").build(), USER_ID);

        assertThat(response.getStatus()).isEqualTo("ASK_MORE");
        assertThat(response.getAssistantMessage()).contains("cần thêm một vài thông tin");
        assertThat(response.getQuestions()).hasSize(3);
        assertThat(response.getQuestions().getFirst()).isInstanceOf(Map.class);
        Map<?, ?> firstQuestion = (Map<?, ?>) response.getQuestions().getFirst();
        assertThat(firstQuestion.get("questionKey")).isEqualTo("childAgeMonths");
        assertThat(firstQuestion.get("answerType")).isEqualTo("NUMBER");
        assertThat(firstQuestion.get("text")).isEqualTo("Bé hiện bao nhiêu tháng tuổi?");
    }

    @Test
    void javaFallbackAskMore_withCoreFieldsPresent_shouldStillReturnRenderablePrompt() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenReturn(ChildTriageResult.builder()
                .status("NEED_MORE_INFO")
                .questions(java.util.List.of("Mô tả cụ thể hơn"))
                .build());

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("triệu chứng chưa rõ")
                        .currentIntake(Map.of(
                                "childAgeMonths", 12,
                                "breathingStatus", "Bình thường",
                                "consciousnessStatus", "Tỉnh táo",
                                "feedingStatus", "Bú tốt"))
                        .build(), USER_ID);

        Map<?, ?> question = (Map<?, ?>) response.getQuestions().getFirst();
        assertThat(question.get("questionKey")).isEqualTo("parentFreeText");
        assertThat(question.get("answerType")).isEqualTo("TEXT");
        assertThat(question.get("text").toString()).contains("mô tả cụ thể hơn");
    }

    @Test
    void continueConversation_shouldRejectAnswerOutsideOutstandingQuestions() {
        IntakeSession session = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","mergedIntake":{"childAgeMonths":8},
                 "questions":[{"questionKey":"duration","text":"Bao lâu?","answerType":"TEXT","options":[]}],"round":2}
                """);
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service().continueConversation(
                ContinueIntakeConversationRequest.builder()
                        .intakeSessionId(session.getId().toString())
                        .newAnswers(Map.of("symptomList", java.util.List.of("difficulty_breathing")))
                        .build(), USER_ID))
                .isInstanceOf(Exception.class);
        verify(childTriageAiClient, never()).continueIntake(any());
    }

    @Test
    void getResult_shouldUnwrapConversationExplainabilityAndFilterMalformedCitation() {
        IntakeSession session = conversationSession(IntakeStatus.COMPLETED, """
                {"status":"TRIAGE_COMPLETE","triageResult":{"riskLevel":"RED","emergencyActionRequired":true,
                 "normalizedSymptoms":["difficulty_breathing"],"matchedRules":["RED_BREATHING_DISTRESS"],
                 "evidenceIds":["WHO_1"],"recommendationCode":"SEEK_EMERGENCY_CARE","ruleSetVersion":"rules-1","responseSchemaVersion":"2.0",
                 "citations":[
                   {"sourceId":"WHO_1","title":"WHO danger signs","organization":"WHO","url":"https://who.int/health-topics/child-health","domain":"who.int","excerpt":"danger signs","sourceVersion":"1","lastReviewed":"2026-07-10","section":"Danger signs","matchedSymptoms":["difficulty_breathing"],"matchedRules":["RED_BREATHING_DISTRESS"],"sourceStatus":"APPROVED","retrievedAt":"2026-07-10T00:00:00Z","retrievalMode":"LOCAL"},
                   {"title":"Bad","source":"Blog","url":"https://example.com/post","excerpt":"bad"}]}}
                """);
        session.setRiskLevel(RiskLevel.RED);
        when(intakeSessionRepository.findByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));

        TriageResultResponse response = service().getResult(session.getId(), USER_ID);

        assertThat(response.getNormalizedSymptoms()).containsExactly("difficulty_breathing");
        assertThat(response.getMatchedRules()).containsExactly("RED_BREATHING_DISTRESS");
        assertThat(response.getEvidenceIds()).containsExactly("WHO_1");
        assertThat(response.getRecommendationCode()).isEqualTo("SEEK_EMERGENCY_CARE");
        assertThat(response.getCitations()).hasSize(1);
    }

    @Test
    void terminalContinue_shouldNotCallAiPersistOrEmitSecondEvent() {
        IntakeSession session = conversationSession(IntakeStatus.COMPLETED, "{\"status\":\"TRIAGE_COMPLETE\"}");
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service().continueConversation(ContinueIntakeConversationRequest.builder()
                .intakeSessionId(session.getId().toString()).newAnswers(Map.of("duration", "2 ngay")).build(), USER_ID))
                .isInstanceOf(TriageException.class)
                .extracting(exception -> ((TriageException) exception).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(childTriageAiClient, never()).continueIntake(any());
        verify(intakeSessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void failedTerminalContinue_shouldRejectWithoutReprocessing() {
        IntakeSession session = conversationSession(IntakeStatus.FAILED, "{\"status\":\"FAILED\"}");
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service().continueConversation(ContinueIntakeConversationRequest.builder()
                .intakeSessionId(session.getId().toString()).newAnswers(Map.of("duration", "2 ngay")).build(), USER_ID))
                .isInstanceOf(TriageException.class)
                .extracting(exception -> ((TriageException) exception).getHttpStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(childTriageAiClient, never()).continueIntake(any());
        verify(intakeSessionRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void duplicateCompletedContinue_shouldNeverPublishASecondCompletionEvent() {
        IntakeSession session = conversationSession(IntakeStatus.COMPLETED, "{\"status\":\"TRIAGE_COMPLETE\"}");
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));
        ContinueIntakeConversationRequest duplicate = ContinueIntakeConversationRequest.builder()
                .intakeSessionId(session.getId().toString())
                .newAnswers(Map.of("duration", "2 ngay"))
                .build();

        assertThatThrownBy(() -> service().continueConversation(duplicate, USER_ID)).isInstanceOf(TriageException.class);
        assertThatThrownBy(() -> service().continueConversation(duplicate, USER_ID)).isInstanceOf(TriageException.class);

        verify(childTriageAiClient, never()).continueIntake(any());
        verify(intakeSessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(IntakeSessionCompleted.class));
    }

    @Test
    void duplicateAskMoreContinue_shouldReturnPriorEnvelopeWithoutSideEffects() {
        IntakeSession session = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","intakeSessionId":"00000000-0000-0000-0000-000000000001",
                 "mergedIntake":{"childAgeMonths":8},"questions":[{"questionKey":"duration"}],"round":2}
                """);
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));

        IntakeConversationResponse response = service().continueConversation(ContinueIntakeConversationRequest.builder()
                .intakeSessionId(session.getId().toString()).newAnswers(Map.of("childAgeMonths", 8)).build(), USER_ID);

        assertThat(response.getRound()).isEqualTo(2);
        verify(childTriageAiClient, never()).continueIntake(any());
        verify(intakeSessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void legacyOneShotMissingVersions_shouldMapWithoutError() {
        IntakeSession session = conversationSession(IntakeStatus.COMPLETED,
                "{\"riskLevel\":\"GREEN\",\"summary\":\"legacy\",\"citations\":[]}");
        session.setRiskLevel(RiskLevel.GREEN);
        when(intakeSessionRepository.findByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));

        TriageResultResponse response = service().getResult(session.getId(), USER_ID);
        assertThat(response.getRiskLevel()).isEqualTo("GREEN");
        assertThat(response.getResponseSchemaVersion()).isEqualTo("1.0");
        assertThat(response.getGraphVersion()).isNull();
    }

    @Test
    void getResult_legacySessionWithoutStageOrMotherProfile_shouldRemainReadable() {
        IntakeSession session = conversationSession(IntakeStatus.COMPLETED,
                "{\"riskLevel\":\"GREEN\",\"summary\":\"legacy\",\"citations\":[]}");
        session.setRiskLevel(RiskLevel.GREEN);
        session.setStage(null);
        session.setMotherProfileId(null);
        when(intakeSessionRepository.findByIdAndUserId(session.getId(), USER_ID)).thenReturn(Optional.of(session));

        TriageResultResponse response = service().getResult(session.getId(), USER_ID);

        assertThat(response.getStage()).isEqualTo(TriageStage.INFANT.name());
        assertThat(response.getRiskLevel()).isEqualTo("GREEN");
    }

    @Test
    void completedEnvelopeWithInvalidRisk_shouldUseFallbackInsteadOfPersistingNullRisk() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"TRIAGE_COMPLETE","mergedIntake":{"breathingStatus":"kho tho"},"triageResult":{"riskLevel":"UNKNOWN"}}
                """);
        when(triageGraphService.run(any())).thenReturn(redResult());

        service().startConversation(StartIntakeConversationRequest.builder().initialText("be kho tho").build(), USER_ID);

        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
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

    private ChildTriageResult redResult() {
        return ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel("RED")
                .riskColor("#EF4444")
                .summary("Immediate danger sign")
                .recommendedAction("Seek emergency care")
                .emergencyActionRequired(true)
                .redFlags(java.util.List.of("Difficulty breathing"))
                .matchedRules(java.util.List.of("RED_BREATHING_DISTRESS"))
                .citations(java.util.List.of())
                .disclaimer("Risk classification only.")
                .questions(java.util.List.of())
                .build();
    }

    private IntakeSession conversationSession(IntakeStatus status, String rawResponse) {
        return TriageTestFactory.makeIntakeSession(session -> {
            session.setStatus(status);
            session.setRawAiResponse(rawResponse);
            session.setSymptoms("CONVERSATION_INTAKE");
            session.setCreatedAt(Instant.parse("2026-07-13T00:00:00Z"));
        });
    }

    private BusinessException consentError(String code) {
        return new BusinessException(HttpStatus.CONFLICT, code, "Postpartum eligibility required");
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
