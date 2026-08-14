package com.carebridge.backend.triage;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.triage.dto.request.TriageAnswerSelection;
import com.carebridge.backend.triage.dto.request.TriageSessionContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageSessionStartRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.TriageDisclaimerPolicy;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.IntakeSessionWriter;
import com.carebridge.backend.triage.rules.IndependentGlobalSafetyFallback;
import com.carebridge.backend.triage.rules.CanonicalAnswerMapper;
import com.carebridge.backend.triage.rules.QuestionCatalog;
import com.carebridge.backend.triage.rules.TriageRuleRegistry;
import com.carebridge.backend.triage.rules.TriageRule;
import com.carebridge.backend.triage.rules.TriageReadinessService;
import com.carebridge.backend.triage.service.ITriageConsentService;
import com.carebridge.backend.triage.service.TriageWorkflowClient;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.TriageMetrics;
import com.carebridge.backend.triage.service.LifecycleBinding;
import com.carebridge.backend.triage.service.LifecycleIntakeBindingService;
import com.carebridge.backend.triage.service.impl.CanonicalTriageSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanonicalTriageSessionServiceTest {
    private static final UUID USER = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final String HASH = "a".repeat(64);

    private IIntakeSessionRepository repository;
    private IntakeSessionWriter writer;
    private TriageWorkflowClient workflow;
    private TriageReadinessService readiness;
    private ITriageConsentService consent;
    private CanonicalTriageSessionService service;
    private EvidenceSourceService evidenceSources;
    private BabyProfileRepository babyProfiles;
    private TriageMetrics metrics;
    private ApplicationEventPublisher eventPublisher;
    private TriageRuleRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = mock(IIntakeSessionRepository.class);
        writer = mock(IntakeSessionWriter.class);
        workflow = mock(TriageWorkflowClient.class);
        readiness = mock(TriageReadinessService.class);
        consent = mock(ITriageConsentService.class);
        evidenceSources = mock(EvidenceSourceService.class);
        babyProfiles = mock(BabyProfileRepository.class);
        metrics = new TriageMetrics();
        eventPublisher = mock(ApplicationEventPublisher.class);
        registry = mock(TriageRuleRegistry.class);
        when(registry.rulesetSha256()).thenReturn(HASH);
        TriageRule testRule = new TriageRule(
                "R_TEST", "1.0.0", "CLINICAL", "Test rule", List.of("PREGNANCY"),
                "YELLOW", 1, 1, true, "ENGINE", null, List.of(), List.of(),
                List.of(), "TEST", "EARLY_CLINICAL_ASSESSMENT", List.of(),
                "DEVELOPMENT_REVIEWED", "CLINICAL_VALIDATION");
        when(registry.byId("R_TEST")).thenReturn(Optional.of(testRule));
        when(registry.safetyPolicies()).thenReturn(List.of());
        when(registry.signalDisplayText()).thenReturn(Map.of(
                "SEIZURE", "Seizure",
                "SEVERE_BREATHING_DIFFICULTY", "Breathing",
                "ALTERED_CONSCIOUSNESS", "Consciousness",
                "CYANOSIS", "Cyanosis",
                "DIZZINESS", "Dizziness",
                "SEVERE_HEADACHE", "Headache"));
        when(readiness.isReady()).thenReturn(true);
        when(readiness.registry()).thenReturn(Optional.of(registry));
        when(readiness.statusReport()).thenReturn(Map.of(
                "technicalStatus", "READY", "publicReleaseStatus", "BLOCKED"));
        TriageDisclaimerPolicy disclaimer = new TriageDisclaimerPolicy("V2", "Thông tin tham khảo");
        QuestionCatalog catalog = new QuestionCatalog();
        service = new CanonicalTriageSessionService(repository, writer, workflow, readiness,
                new IndependentGlobalSafetyFallback(), consent, disclaimer, catalog,
                new CanonicalAnswerMapper(catalog, CanonicalAnswerMapper.MAPPING_RESOURCE),
                evidenceSources, babyProfiles, metrics, eventPublisher, mapper, true, 30);
        when(babyProfiles.findByIdAndOwnerUserId(USER, USER))
                .thenReturn(Optional.of(mock(BabyProfile.class)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void startUsesExistingTableHandshakeAndRedactsHealthTextBeforePersistence() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation ->
                new TriageWorkflowClient.WorkflowResult(
                        graphState(inserted, "YELLOW", false), "READY", "2.2.0", HASH));

        var response = service.start(startRequest("đau bụng riêng tư"), USER);

        assertThat(response.stateVersion()).isEqualTo(1);
        assertThat(response.outcome()).isEqualTo("YELLOW");
        assertThat(inserted.get().getSchemaVersion()).isEqualTo("triage-v2-1");
        assertThat(inserted.get().getResultJson())
                .contains("\"triageState\"")
                .doesNotContain("\"v2State\"");
        assertThat(inserted.get().getResultJson()).contains("REDACTED_HEALTH_TEXT")
                .doesNotContain("đau bụng riêng tư");
        verify(consent).ensureActiveConsent(USER);
    }

    @Test
    void plannedQuestionsExposeVietnameseLabelsFromCanonicalCatalog() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "NEEDS_MORE_INFO", false);
            state.put("plannedQuestionIds", List.of("Q_GLOBAL_DANGER"));
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("be sot"), USER);

        assertThat(response.questions()).containsExactly("Q_GLOBAL_DANGER");
        assertThat(response.questionDetails()).hasSize(1);
        var question = response.questionDetails().getFirst();
        assertThat(question.questionId()).isEqualTo("Q_GLOBAL_DANGER");
        assertThat(question.text()).contains("dấu hiệu");
        assertThat(question.options()).anySatisfy(option -> {
            assertThat(option.optionCode()).isEqualTo("DANGER_NONE");
            assertThat(option.displayText()).isEqualTo("Không có dấu hiệu nào");
        });
    }

    @Test
    void numericBabyAgeAnswerIsValidatedAndForwardedAsContext() throws Exception {
        IntakeSession session = persistedSession(1, "request_previous_1234",
                "NEEDS_MORE_INFO", "BABY", "UNKNOWN");
        UUID id = session.getId();
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = mapper.readValue(session.getResultJson(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> priorState = (Map<String, Object>) envelope.get("v2State");
        priorState.put("plannedQuestionIds", List.of("Q_BABY_AGE_MONTHS"));
        session.setResultJson(mapper.writeValueAsString(envelope));
        when(repository.findForUpdateByIdAndUserId(id, USER)).thenReturn(Optional.of(session));
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> previous =
                    (Map<String, Object>) invocation.<Map<String, Object>>getArgument(0)
                            .get("previousState");
            assertThat(previous.get("babyAgeMonths")).isEqualTo(2);
            Map<String, Object> state =
                    graphState(new AtomicReference<>(session), "YELLOW", false);
            state.put("stateVersion", 1);
            state.put("expectedStateVersion", 1);
            state.put("targetEntity", "BABY");
            state.put("stage", "INFANT_0_12M");
            return new TriageWorkflowClient.WorkflowResult(
                    state, "READY", "2.2.0", HASH);
        });

        service.continueSession(new TriageSessionContinueRequest(
                id, 1, "Tuổi của bé (tháng): 2", "message_1234567899",
                "request_1234567899",
                List.of(new TriageAnswerSelection("Q_BABY_AGE_MONTHS", null,
                        java.math.BigDecimal.valueOf(2))), Map.of(), Map.of()), USER);

        verify(workflow).executeTurn(any());
    }

    @Test
    void mixedNumericAndOptionAnswersKeepTheirTransportLedgersAligned() throws Exception {
        IntakeSession session = persistedSession(1, "request_previous_1234",
                "NEEDS_MORE_INFO", "BABY", "INFANT_0_12M");
        UUID id = session.getId();
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = mapper.readValue(session.getResultJson(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> priorState = (Map<String, Object>) envelope.get("v2State");
        priorState.put("plannedQuestionIds", List.of("Q_BABY_AGE_MONTHS", "Q_GLOBAL_DANGER"));
        session.setResultJson(mapper.writeValueAsString(envelope));
        when(repository.findForUpdateByIdAndUserId(id, USER)).thenReturn(Optional.of(session));

        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> payload = invocation.getArgument(0);
            assertThat(payload.get("answeredQuestionIds"))
                    .isEqualTo(List.of("Q_BABY_AGE_MONTHS", "Q_GLOBAL_DANGER"));
            assertThat(payload.get("submittedOptionQuestionIds"))
                    .isEqualTo(List.of("Q_GLOBAL_DANGER"));
            assertThat(payload.get("submittedOptionCodes")).isEqualTo(List.of("DANGER_NONE"));
            Map<String, Object> state = graphState(new AtomicReference<>(session), "YELLOW", true);
            state.put("stateVersion", 1);
            state.put("expectedStateVersion", 1);
            state.put("targetEntity", "BABY");
            state.put("stage", "INFANT_0_12M");
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        service.continueSession(new TriageSessionContinueRequest(
                id, 1, "Bé 2 tháng, không có dấu hiệu nguy hiểm", "message_1234567899",
                "request_1234567899", List.of(
                        new TriageAnswerSelection("Q_BABY_AGE_MONTHS", null, BigDecimal.valueOf(2)),
                        new TriageAnswerSelection("Q_GLOBAL_DANGER", "DANGER_NONE", null)),
                Map.of(), Map.of()), USER);

        verify(workflow).executeTurn(any());
    }

    @Test
    void numericBabyAgeOutsideSupportedRangeIsRejectedBeforeWorkflow() throws Exception {
        IntakeSession session = persistedSession(1, "request_previous_1234",
                "NEEDS_MORE_INFO", "BABY", "UNKNOWN");
        UUID id = session.getId();
        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = mapper.readValue(session.getResultJson(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> priorState = (Map<String, Object>) envelope.get("v2State");
        priorState.put("plannedQuestionIds", List.of("Q_BABY_AGE_MONTHS"));
        session.setResultJson(mapper.writeValueAsString(envelope));
        when(repository.findForUpdateByIdAndUserId(id, USER)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.continueSession(
                new TriageSessionContinueRequest(
                        id, 1, "Tuổi của bé (tháng): -1", "message_1234567899",
                        "request_1234567899",
                        List.of(new TriageAnswerSelection("Q_BABY_AGE_MONTHS", null,
                                java.math.BigDecimal.valueOf(-1))), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .hasMessageContaining("ngoài phạm vi");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void canonicalStartPersistsValidatedLifecycleBinding() {
        UUID journeyId = UUID.fromString("68000000-0000-0000-0000-000000000002");
        LifecycleIntakeBindingService lifecycle = mock(LifecycleIntakeBindingService.class);
        LifecycleBinding binding = new LifecycleBinding(
                journeyId, OriginDashboard.MOTHER_JOURNEY, journeyId, TriageStage.PREGNANCY,
                UUID.fromString("68000000-0000-0000-0000-000000000004"),
                Instant.now().plus(7, ChronoUnit.DAYS));
        when(lifecycle.bindForStart(any(), any(), any())).thenReturn(binding);
        ReflectionTestUtils.setField(service, "lifecycleBindingService", lifecycle);
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation ->
                new TriageWorkflowClient.WorkflowResult(
                        graphState(inserted, "YELLOW", false), "READY", "2.2.0", HASH));

        service.start(new TriageSessionStartRequest(
                USER, "MOTHER", "PREGNANCY", Map.of(), "dau dau",
                "message_1234567890", "request_1234567890", Map.of(), Map.of(), Map.of(),
                journeyId, OriginDashboard.MOTHER_JOURNEY, journeyId), USER);

        assertThat(inserted.get().getJourneyId()).isEqualTo(journeyId);
        assertThat(inserted.get().getOriginDashboard()).isEqualTo(OriginDashboard.MOTHER_JOURNEY);
        assertThat(inserted.get().getContinuationToken()).isEqualTo(binding.continuationToken());
        verify(lifecycle).recordCreated();
    }

    @Test
    void terminalYellowPublishesCompletionExactlyOnceAcrossStartReplay() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation ->
                new TriageWorkflowClient.WorkflowResult(
                        graphState(inserted, "YELLOW", true), "READY", "2.2.0", HASH));

        var first = service.start(startRequest("dau dau"), USER);
        var replay = service.start(startRequest("dau dau"), USER);

        assertThat(first.stop()).isTrue();
        assertThat(replay).isEqualTo(first);
        assertThat(inserted.get().getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(inserted.get().getStage()).isEqualTo(TriageStage.PREGNANCY);
        verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, never()).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void realPythonClarificationStateIsAcceptedWithoutFallback() throws Exception {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        try (InputStream stream = getClass().getResourceAsStream(
                "/triage/python_clarification_state_v2.json")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> captured = mapper.readValue(stream, Map.class);
            when(workflow.executeTurn(any())).thenAnswer(invocation -> {
                Map<String, Object> state = new LinkedHashMap<>(captured);
                state.put("sessionId", inserted.get().getId().toString());
                state.put("rulesetHash", HASH);
                return new TriageWorkflowClient.WorkflowResult(
                        state, "READY", "2.2.0", HASH);
            });
        }

        var response = service.start(startRequest("Em thay kho chiu"), USER);

        assertThat(response.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(response.action()).isEqualTo("ASK_CLARIFYING_QUESTIONS");
        assertThat(response.questions()).containsExactly("Q_CLARIFY_INTENT", "Q_GLOBAL_DANGER");
        assertThat(response.stop()).isFalse();
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("READY");
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void pythonCoverageFieldsRejectNestedFreeTextAtTheJavaBoundary() throws Exception {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        try (InputStream stream = getClass().getResourceAsStream(
                "/triage/python_clarification_state_v2.json")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> captured = mapper.readValue(stream, Map.class);
            captured.put("coverageLimitations", List.of(Map.of("healthText", "raw symptom")));
            when(workflow.executeTurn(any())).thenAnswer(invocation -> {
                Map<String, Object> state = new LinkedHashMap<>(captured);
                state.put("sessionId", inserted.get().getId().toString());
                state.put("rulesetHash", HASH);
                return new TriageWorkflowClient.WorkflowResult(
                        state, "READY", "2.2.0", HASH);
            });
        }

        assertThatThrownBy(() -> service.start(startRequest("Em thay kho chiu"), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_INVALID_WORKFLOW_RESPONSE");
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void ownedSelectedTargetReachesPythonWorkflowWithoutCallerClinicalContext() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        AtomicReference<Map<String, Object>> workflowPayload = new AtomicReference<>();
        AtomicReference<TriageStage> stageAtExecution = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_3234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            workflowPayload.set(invocation.getArgument(0));
            stageAtExecution.set(inserted.get().getStage());
            return new TriageWorkflowClient.WorkflowResult(
                    graphState(inserted, "YELLOW", false), "READY", "2.2.0", HASH);
        });

        service.start(new TriageSessionStartRequest(
                USER, "BABY", "INFANT_0_12M", Map.of(),
                "baby symptom", "message_3234567890", "request_3234567890",
                Map.of(), Map.of(), Map.of()), USER);

        assertThat(workflowPayload.get().get("selectedTarget")).isEqualTo("BABY");
        assertThat(workflowPayload.get().get("journeyContext"))
                .isEqualTo(Map.of("stage", "INFANT_0_12M"));
        assertThat(stageAtExecution.get()).isEqualTo(TriageStage.INFANT);
    }

    @Test
    void mismatchedMobileConsentVersionIsRejectedByJavaAuthority() {
        assertThatThrownBy(() -> service.start(new TriageSessionStartRequest(
                USER, "MOTHER", null, Map.of(), "text", "message_2234567890",
                "request_2234567890", Map.of("disclaimerVersion", "STALE_MOBILE_VERSION"),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_CONSENT_CONTEXT_MISMATCH");
        verify(consent).ensureActiveConsent(USER);
        verify(repository, never()).save(any());
    }

    @Test
    void journeyContextRejectsUnknownFieldsBeforePersistence() {
        assertThatThrownBy(() -> service.start(new TriageSessionStartRequest(
                USER, "MOTHER", null, Map.of("note", "private health text"), "text",
                "message_6234567890", "request_6234567890", Map.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_UNTRUSTED_CLINICAL_STATE");
        verify(repository, never()).save(any());
    }

    @Test
    void selectedBabyProfileMustBelongToCurrentUser() {
        UUID foreignProfile = UUID.fromString("20000000-0000-0000-0000-000000000004");
        when(babyProfiles.findByIdAndOwnerUserId(foreignProfile, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(new TriageSessionStartRequest(
                foreignProfile, "BABY", null, Map.of(), "text", "message_7234567890",
                "request_7234567890", Map.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_PROFILE_FORBIDDEN");
        verify(repository, never()).save(any());
    }

    @Test
    void duplicateStartReturnsPersistedResponseWithoutCallingPythonAgain() throws Exception {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation ->
                new TriageWorkflowClient.WorkflowResult(
                        graphState(inserted, "NEEDS_MORE_INFO", false), "READY", "2.2.0", HASH));

        service.start(startRequest("không gửi lại"), USER);
        var response = service.start(startRequest("không gửi lại"), USER);

        assertThat(response.stateVersion()).isEqualTo(1);
        verify(workflow).executeTurn(any());
    }

    @Test
    void duplicateStartWithDifferentContentIsRejected() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation ->
                new TriageWorkflowClient.WorkflowResult(
                        graphState(inserted, "NEEDS_MORE_INFO", false), "READY", "2.2.0", HASH));
        service.start(startRequest("đau đầu"), USER);

        assertThatThrownBy(() -> service.start(startRequest("đang co giật"), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_IDEMPOTENCY_KEY_CONFLICT");
    }

    @Test
    void staleContinueIsRejectedBeforeWorkflowExecution() throws Exception {
        IntakeSession existing = persistedSession(3, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 2, "trả lời", "message_2234567890",
                "request_2234567890", List.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_STATE_VERSION_CONFLICT");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void callerCannotSelfDeclareGlobalDangerOrAbsence() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "khó thở", "message_3234567890", "request_3234567890",
                List.of(), Map.of("SEVERE_BREATHING_DIFFICULTY", "PRESENT"), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_UNTRUSTED_CLINICAL_STATE");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void aBatchOfAnswersIsDerivedServerSideIntoSignalsAndAnsweredQuestions() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO",
                "MOTHER", "PREGNANCY");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));
        AtomicReference<Map<String, Object>> payload = new AtomicReference<>();
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            payload.set(invocation.getArgument(0));
            Map<String, Object> nextState = graphState(
                    new AtomicReference<>(existing), "YELLOW", false);
            nextState.put("stateVersion", 1);
            nextState.put("expectedStateVersion", 1);
            return new TriageWorkflowClient.WorkflowResult(
                    nextState, "READY", "2.2.0", HASH);
        });

        // The chat asks up to three questions per round, so all three answers arrive together and
        // must cost one state version, not three.
        service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "tra loi", "message_5234567890", "request_5234567890",
                List.of(new TriageAnswerSelection("Q_GLOBAL_DANGER", "DANGER_SEIZURE"),
                        new TriageAnswerSelection("Q_DIZZINESS", "DIZZINESS_NO")),
                Map.of(), Map.of()), USER);

        assertThat(strings(payload.get().get("answeredQuestionIds")))
                .containsExactly("Q_GLOBAL_DANGER", "Q_DIZZINESS");
        assertThat(strings(payload.get().get("submittedOptionCodes")))
                .containsExactly("DANGER_SEIZURE", "DIZZINESS_NO");
        assertThat(objectMap(payload.get().get("signals"))).containsKeys("SEIZURE", "DIZZINESS");
    }

    @Test
    void answeringTheSameQuestionTwiceInOneTurnIsRejected() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));

        // Two answers to one question have no defined winner; silently taking the last would let a
        // client overwrite an earlier danger answer with a benign one.
        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "tra loi", "message_6234567890", "request_6234567890",
                List.of(new TriageAnswerSelection("Q_GLOBAL_DANGER", "DANGER_SEIZURE"),
                        new TriageAnswerSelection("Q_GLOBAL_DANGER", "DANGER_NONE")),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_DUPLICATE_ANSWER");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void aForgedQuestionIdInABatchIsRejectedBeforeTheWorkflowRuns() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "tra loi", "message_7234567890", "request_7234567890",
                List.of(new TriageAnswerSelection("Q_NOT_A_REAL_QUESTION", "ANYTHING")),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_UNKNOWN_QUESTION");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void exactContinuationReplayReturnsStoredResponseWithoutCallingPythonAgain() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> nextState = graphState(
                    new AtomicReference<>(existing), "YELLOW", false);
            nextState.put("stateVersion", 1);
            nextState.put("expectedStateVersion", 1);
            return new TriageWorkflowClient.WorkflowResult(
                    nextState, "READY", "2.2.0", HASH);
        });
        TriageSessionContinueRequest request = new TriageSessionContinueRequest(
                existing.getId(), 1, "khong biet", "message_1034567890",
                "request_1034567890", List.of(), Map.of(), Map.of());

        service.continueSession(request, USER);
        var replay = service.continueSession(request, USER);

        assertThat(replay.stateVersion()).isEqualTo(2);
        verify(workflow).executeTurn(any());
    }

    @Test
    void continuationReplayWithChangedContentIsRejected() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> nextState = graphState(
                    new AtomicReference<>(existing), "YELLOW", false);
            nextState.put("stateVersion", 1);
            nextState.put("expectedStateVersion", 1);
            return new TriageWorkflowClient.WorkflowResult(
                    nextState, "READY", "2.2.0", HASH);
        });
        service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "khong biet", "message_1134567890",
                "request_1134567890", List.of(), Map.of(), Map.of()), USER);

        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "noi dung khac", "message_1134567890",
                "request_1134567890", List.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_IDEMPOTENCY_KEY_CONFLICT");
        verify(workflow).executeTurn(any());
    }

    @Test
    void aRealButUnplannedQuestionIsRejectedBeforeTheWorkflowRuns() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "tra loi", "message_8234567890", "request_8234567890",
                List.of(new TriageAnswerSelection("Q_VISUAL_CHANGE", "VISUAL_CHANGE_NO")),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_ANSWER_NOT_PLANNED");
        verify(workflow, never()).executeTurn(any());
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        return value instanceof List<?> list
                ? list.stream().map(String::valueOf).toList() : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @Test
    void pythonFailureWithoutDangerNeverBecomesGreenOrOutOfScope() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER)).thenReturn(Optional.of(existing));
        when(workflow.executeTurn(any())).thenThrow(new IllegalStateException("down"));

        var response = service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "không rõ", "message_4234567890", "request_4234567890",
                List.of(), Map.of(), Map.of()), USER);

        assertThat(response.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(response.outcome()).isNotIn("GREEN", "OUT_OF_SCOPE");
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("FALLBACK_ONLY");
    }

    @Test
    void pythonFailureStillEscalatesUnambiguousDangerFromFreeText() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenThrow(new IllegalStateException("down"));

        var response = service.start(startRequest("Toi dang co giat"), USER);

        assertThat(response.outcome()).isEqualTo("RED");
        assertThat(response.stop()).isTrue();
        assertThat(inserted.get().getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(inserted.get().isEmergency()).isTrue();
        verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("FALLBACK_ONLY");
    }

    @Test
    void continuationRechecksConsentBeforeCallingTheWorkflow() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));
        org.mockito.Mockito.doThrow(new TriageException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "TRIAGE_CONSENT_REQUIRED", "Consent expired"))
                .when(consent).ensureActiveConsent(USER);

        assertThatThrownBy(() -> service.continueSession(new TriageSessionContinueRequest(
                existing.getId(), 1, "khong biet", "message_9234567890", "request_9234567890",
                List.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_CONSENT_REQUIRED");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void freeTextCannotHideInsideStructuredSignalOrMeasurement() {
        for (Map<String, Object> signals : List.of(
                Map.<String, Object>of("SEIZURE", Map.of(
                        "presence", "PRESENT", "note", "raw PII")),
                Map.<String, Object>of("PATIENT_JANE_DOE_HAS_HIV", "PRESENT"))) {
            assertThatThrownBy(() -> service.start(new TriageSessionStartRequest(
                    USER, "MOTHER", null, Map.of(), "text", "message_5234567890",
                    "request_5234567890", Map.of("disclaimerVersion", "V2"),
                    signals, Map.of()), USER))
                    .isInstanceOf(TriageException.class)
                    .extracting("code").isEqualTo("TRIAGE_UNTRUSTED_CLINICAL_STATE");
        }
    }

    @Test
    void sourceVerifiedCitationWithApprovedDeepLinkIsReturned() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(evidenceSources.isApprovedDeepLink(any())).thenReturn(true);
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "YELLOW", false);
            state.put("decisiveRuleIds", List.of("R_TEST"));
            state.put("citations", List.of(verifiedCitation("SOURCE_VERIFIED")));
            state.put("rationale", "Kết quả Vàng vì có dấu hiệu cần được đánh giá sớm.");
            state.put("evidenceStatus", "AVAILABLE");
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.citations()).singleElement()
                .satisfies(citation -> {
                    assertThat(citation.get("sourceStatus")).isEqualTo("SOURCE_VERIFIED");
                    assertThat(citation.get("retrievalMode")).isEqualTo("LOCAL_BM25");
                });
        assertThat(response.evidenceStatus()).isEqualTo("AVAILABLE");
        assertThat(response.rationale()).contains("Kết quả Vàng");
        verify(evidenceSources, atLeastOnce()).isApprovedDeepLink(any());
    }

    @Test
    void unverifiedCitationIsRejectedWithoutDowngradingOutcome() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "YELLOW", false);
            state.put("citations", List.of(verifiedCitation("PENDING")));
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.outcome()).isEqualTo("YELLOW");
        assertThat(response.citations()).isEmpty();
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("READY");
        verify(evidenceSources, never()).isApprovedDeepLink(any());
        assertThat(metrics.failureCount(TriageMetrics.Failure.CITATION_REJECTED)).isEqualTo(1);
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void unknownWorkflowFieldFailsClosedBeforePersistence() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "YELLOW", false);
            state.put("rawPatientNote", "private health text");
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        assertThatThrownBy(() -> service.start(startRequest("dau bung"), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_INVALID_WORKFLOW_RESPONSE");
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void phase2dConversationFieldsAreAllowedInPersistedWorkflowState() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "NEEDS_MORE_INFO", false);
            state.put("plannedQuestionIds", List.of("Q_BABY_TEMPERATURE"));
            state.put("askedQuestionIds", List.of("Q_BABY_TEMPERATURE"));
            state.put("confirmedConversationIntent", "SYMPTOM_TRIAGE");
            state.put("targetEntity", "BABY");
            state.put("stage", "INFANT_0_12M");
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("bé hai tháng bị sốt"), USER);

        assertThat(response.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(response.questions()).containsExactly("Q_BABY_TEMPERATURE");
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("READY");
        assertThat(inserted.get().getResultJson())
                .contains("askedQuestionIds", "confirmedConversationIntent", "SYMPTOM_TRIAGE");
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void pythonTextReportedNumericStateIsAcceptedWithoutFallback() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "RED", true);
            state.put("requiredAction", "IMMEDIATE_EMERGENCY_ASSESSMENT");
            state.put("targetEntity", "BABY");
            state.put("stage", "INFANT_0_12M");
            state.put("babyAgeMonths", 2);
            state.put("measurements", Map.of(
                    "babyAgeMonths", Map.of(
                            "value", 2, "unit", "MONTHS", "temporalStatus", "CURRENT",
                            "provenance", "USER_REPORTED_TEXT"),
                    "temperatureC", Map.of(
                            "value", 38.2, "unit", "C", "temporalStatus", "CURRENT",
                            "provenance", "USER_REPORTED_TEXT")));
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("Be hai thang do duoc 38,2 do"), USER);

        assertThat(response.outcome()).isEqualTo("RED");
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("READY");
        assertThat(inserted.get().getStage()).isEqualTo(TriageStage.INFANT);
        assertThat(inserted.get().getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        assertThat(inserted.get().getResultJson())
                .contains("babyAgeMonths", "temperatureC", "USER_REPORTED_TEXT");
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void invalidCitationDoesNotEraseAnotherValidRuleMappedCitation() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(evidenceSources.isApprovedDeepLink(any())).thenReturn(true);
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "YELLOW", false);
            state.put("decisiveRuleIds", List.of("R_TEST"));
            Map<String, Object> invalid = new LinkedHashMap<>(verifiedCitation("SOURCE_VERIFIED"));
            invalid.put("sourceId", "INVALID_PENDING");
            invalid.put("sourceStatus", "PENDING");
            state.put("citations", List.of(verifiedCitation("SOURCE_VERIFIED"), invalid));
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.outcome()).isEqualTo("YELLOW");
        assertThat(response.citations()).hasSize(1);
        assertThat(response.evidenceStatus()).isEqualTo("AVAILABLE");
        assertThat(metrics.failureCount(TriageMetrics.Failure.CITATION_REJECTED)).isEqualTo(1);
        assertThat(metrics.failureCount(TriageMetrics.Failure.FALLBACK)).isZero();
    }

    @Test
    void redWorkflowMustStopWithAnEmergencyAction() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            Map<String, Object> state = graphState(inserted, "RED", false);
            state.put("requiredAction", "ROUTE_TO_HEALTHCARE_WORKER");
            return new TriageWorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        assertThatThrownBy(() -> service.start(startRequest("co giat"), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_INVALID_WORKFLOW_RESPONSE");
        verify(eventPublisher, never()).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReprojectsPersistedStateInsteadOfTrustingStalePublicEvidence() throws Exception {
        IntakeSession stored = persistedSession(3, "request_stale_123456", "YELLOW");
        Map<String, Object> envelope = mapper.readValue(stored.getResultJson(), Map.class);
        Map<String, Object> stale = (Map<String, Object>) envelope.get("publicResponse");
        stale.put("citations", List.of(verifiedCitation("SOURCE_VERIFIED")));
        stale.put("evidenceStatus", "AVAILABLE");
        stored.setResultJson(mapper.writeValueAsString(envelope));
        when(repository.findByIdAndUserId(stored.getId(), USER)).thenReturn(Optional.of(stored));

        var response = service.get(stored.getId(), USER);

        assertThat(response.citations()).isEmpty();
        assertThat(response.evidenceStatus()).isEqualTo("UNAVAILABLE");
        verify(evidenceSources, never()).isApprovedDeepLink(any());
    }

    private TriageSessionStartRequest startRequest(String text) {
        return new TriageSessionStartRequest(USER, "MOTHER", "PREGNANCY", Map.of(), text,
                "message_1234567890", "request_1234567890",
                Map.of(), Map.of(), Map.of());
    }

    private Map<String, Object> verifiedCitation(String status) {
        return Map.ofEntries(
                Map.entry("sourceId", "WHO_TEST"),
                Map.entry("title", "WHO danger signs"),
                Map.entry("organization", "World Health Organization"),
                Map.entry("publisher", "World Health Organization"),
                Map.entry("url", "https://www.who.int/publications/i/item/test"),
                Map.entry("domain", "who.int"),
                Map.entry("section", "Danger signs"),
                Map.entry("contentHash", "b".repeat(64)),
                Map.entry("sourceStatus", status),
                Map.entry("retrievalMode", "LOCAL_BM25"),
                Map.entry("ruleIds", List.of("R_TEST")));
    }

    private Map<String, Object> graphState(AtomicReference<IntakeSession> session,
                                           String outcome, boolean stop) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionId", session.get().getId().toString());
        state.put("stateVersion", 0);
        state.put("expectedStateVersion", 0);
        state.put("targetEntity", "MOTHER");
        state.put("intent", "SYMPTOM_TRIAGE");
        state.put("stage", "PREGNANCY");
        state.put("triageOutcome", outcome);
        state.put("requiredAction", "ROUTE_TO_HEALTHCARE_WORKER");
        state.put("stopConversation", stop);
        state.put("plannedQuestionIds", List.of());
        state.put("scopeStatus", "IN_SCOPE");
        state.put("pendingRiskStatuses", List.of());
        state.put("completionReason", null);
        state.put("rulesetVersion", "2.2.0");
        state.put("rulesetHash", HASH);
        state.put("latestUserMessage", "đau bụng riêng tư");
        state.put("rawMessages", List.of(Map.of("role", "USER", "content", "đau bụng riêng tư")));
        return state;
    }

    private IntakeSession persistedSession(int version, String requestId, String outcome) throws Exception {
        return persistedSession(version, requestId, outcome, "UNKNOWN", "UNKNOWN");
    }

    private IntakeSession persistedSession(int version, String requestId, String outcome,
                                           String targetEntity, String stage) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sessionId", id.toString());
        state.put("stateVersion", version);
        state.put("targetEntity", targetEntity);
        state.put("intent", "UNKNOWN");
        state.put("stage", stage);
        state.put("triageOutcome", outcome);
        state.put("requiredAction", "ASK_CLARIFYING_QUESTIONS");
        state.put("stopConversation", false);
        state.put("plannedQuestionIds", List.of("Q_GLOBAL_DANGER", "Q_DIZZINESS"));
        state.put("scopeStatus", "UNKNOWN");
        state.put("pendingRiskStatuses", List.of());
        // workflowState() only trusts an envelope that carries these two: without them the state
        // is treated as a sparse Java-only fallback and deliberately not resumed.
        state.put("rawMessages", List.of());
        state.put("processedRequestIds", List.of());
        Map<String, Object> publicResponse = mapper.convertValue(
                new com.carebridge.backend.triage.dto.response.TriageSessionResponse(
                        id, version, "UNKNOWN", "UNKNOWN", "UNKNOWN", outcome,
                        "ASK_CLARIFYING_QUESTIONS", false, List.of(), List.of(), "UNKNOWN", List.of(),
                        null, "2.2.0", HASH, "", "UNAVAILABLE", List.of(), "Thông tin tham khảo",
                        Map.of("technicalStatus", "READY")), Map.class);
        Map<String, Object> envelope = Map.ofEntries(
                Map.entry("contract", "triage-v2-1"), Map.entry("stateVersion", version),
                Map.entry("lastRequestId", requestId),
                Map.entry("requestFingerprint", "legacy-fingerprint"),
                Map.entry("v2State", state), Map.entry("publicResponse", publicResponse));
        return IntakeSession.builder().id(id).userId(USER).status(IntakeStatus.NEED_MORE_INFO)
                .schemaVersion("triage-v2-1").resultJson(mapper.writeValueAsString(envelope))
                .createdAt(Instant.now()).createdBy(USER).symptoms("TRIAGE_REDACTED").build();
    }
}
