package com.carebridge.backend.triage;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.triage.dto.request.TriageV2AnswerSelection;
import com.carebridge.backend.triage.dto.request.TriageV2ContinueRequest;
import com.carebridge.backend.triage.dto.request.TriageV2StartRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.policy.TriageDisclaimerPolicy;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.IntakeSessionWriter;
import com.carebridge.backend.triage.rules.IndependentGlobalSafetyFallback;
import com.carebridge.backend.triage.rules.CanonicalAnswerMapper;
import com.carebridge.backend.triage.rules.QuestionCatalog;
import com.carebridge.backend.triage.rules.TriageRuleRegistry;
import com.carebridge.backend.triage.rules.TriageV2ReadinessService;
import com.carebridge.backend.triage.service.ITriageConsentService;
import com.carebridge.backend.triage.service.TriageV2WorkflowClient;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.TriageV2Metrics;
import com.carebridge.backend.triage.service.impl.TriageV2SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.io.InputStream;
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

class TriageV2SessionServiceTest {
    private static final UUID USER = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final String HASH = "a".repeat(64);

    private IIntakeSessionRepository repository;
    private IntakeSessionWriter writer;
    private TriageV2WorkflowClient workflow;
    private TriageV2ReadinessService readiness;
    private ITriageConsentService consent;
    private TriageV2SessionService service;
    private EvidenceSourceService evidenceSources;
    private BabyProfileRepository babyProfiles;
    private TriageV2Metrics metrics;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = mock(IIntakeSessionRepository.class);
        writer = mock(IntakeSessionWriter.class);
        workflow = mock(TriageV2WorkflowClient.class);
        readiness = mock(TriageV2ReadinessService.class);
        consent = mock(ITriageConsentService.class);
        evidenceSources = mock(EvidenceSourceService.class);
        babyProfiles = mock(BabyProfileRepository.class);
        metrics = new TriageV2Metrics();
        TriageRuleRegistry registry = mock(TriageRuleRegistry.class);
        when(registry.rulesetSha256()).thenReturn(HASH);
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
        service = new TriageV2SessionService(repository, writer, workflow, readiness,
                new IndependentGlobalSafetyFallback(), consent, disclaimer, catalog,
                new CanonicalAnswerMapper(catalog, CanonicalAnswerMapper.MAPPING_RESOURCE),
                evidenceSources, babyProfiles, metrics, mapper, true, 30);
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
                new TriageV2WorkflowClient.WorkflowResult(
                        graphState(inserted, "YELLOW", false), "READY", "2.2.0", HASH));

        var response = service.start(startRequest("đau bụng riêng tư"), USER);

        assertThat(response.stateVersion()).isEqualTo(1);
        assertThat(response.outcome()).isEqualTo("YELLOW");
        assertThat(inserted.get().getSchemaVersion()).isEqualTo("triage-v2-1");
        assertThat(inserted.get().getResultJson()).contains("REDACTED_HEALTH_TEXT")
                .doesNotContain("đau bụng riêng tư");
        verify(consent).ensureActiveConsent(USER);
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
                return new TriageV2WorkflowClient.WorkflowResult(
                        state, "READY", "2.2.0", HASH);
            });
        }

        var response = service.start(startRequest("Em thay kho chiu"), USER);

        assertThat(response.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(response.action()).isEqualTo("ASK_CLARIFYING_QUESTIONS");
        assertThat(response.questions()).containsExactly("Q_CLARIFY_INTENT", "Q_GLOBAL_DANGER");
        assertThat(response.stop()).isFalse();
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("READY");
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.FALLBACK)).isZero();
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
                return new TriageV2WorkflowClient.WorkflowResult(
                        state, "READY", "2.2.0", HASH);
            });
        }

        service.start(startRequest("Em thay kho chiu"), USER);

        assertThat(metrics.failureCount(TriageV2Metrics.Failure.FALLBACK)).isEqualTo(1);
    }

    @Test
    void ownedSelectedTargetReachesPythonWorkflowWithoutCallerClinicalContext() {
        AtomicReference<IntakeSession> inserted = new AtomicReference<>();
        AtomicReference<Map<String, Object>> workflowPayload = new AtomicReference<>();
        when(repository.findByUserIdAndClientRequestId(USER, "request_3234567890"))
                .thenAnswer(invocation -> Optional.ofNullable(inserted.get()));
        when(writer.insertConversationIfAbsent(any())).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return new IntakeSessionWriter.InsertResult(true);
        });
        when(workflow.executeTurn(any())).thenAnswer(invocation -> {
            workflowPayload.set(invocation.getArgument(0));
            return new TriageV2WorkflowClient.WorkflowResult(
                    graphState(inserted, "YELLOW", false), "READY", "2.2.0", HASH);
        });

        service.start(new TriageV2StartRequest(
                USER, "BABY", Map.of(),
                "baby symptom", "message_3234567890", "request_3234567890",
                Map.of(), Map.of(), Map.of()), USER);

        assertThat(workflowPayload.get().get("selectedTarget")).isEqualTo("BABY");
        assertThat(workflowPayload.get().get("journeyContext")).isEqualTo(Map.of());
    }

    @Test
    void mismatchedMobileConsentVersionIsRejectedByJavaAuthority() {
        assertThatThrownBy(() -> service.start(new TriageV2StartRequest(
                USER, "MOTHER", Map.of(), "text", "message_2234567890",
                "request_2234567890", Map.of("disclaimerVersion", "STALE_MOBILE_VERSION"),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_CONSENT_CONTEXT_MISMATCH");
        verify(consent).ensureActiveConsent(USER);
        verify(repository, never()).save(any());
    }

    @Test
    void journeyContextRejectsUnknownFieldsBeforePersistence() {
        assertThatThrownBy(() -> service.start(new TriageV2StartRequest(
                USER, "MOTHER", Map.of("note", "private health text"), "text",
                "message_6234567890", "request_6234567890", Map.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_UNTRUSTED_CLINICAL_STATE");
        verify(repository, never()).save(any());
    }

    @Test
    void selectedBabyProfileMustBelongToCurrentUser() {
        UUID foreignProfile = UUID.fromString("20000000-0000-0000-0000-000000000004");
        when(babyProfiles.findByIdAndOwnerUserId(foreignProfile, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(new TriageV2StartRequest(
                foreignProfile, "BABY", Map.of(), "text", "message_7234567890",
                "request_7234567890", Map.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_PROFILE_FORBIDDEN");
        verify(repository, never()).save(any());
    }

    @Test
    void duplicateStartReturnsPersistedResponseWithoutCallingPythonAgain() throws Exception {
        IntakeSession existing = persistedSession(2, "request_1234567890", "NEEDS_MORE_INFO");
        when(repository.findByUserIdAndClientRequestId(USER, "request_1234567890"))
                .thenReturn(Optional.of(existing));

        var response = service.start(startRequest("không gửi lại"), USER);

        assertThat(response.stateVersion()).isEqualTo(2);
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void staleContinueIsRejectedBeforeWorkflowExecution() throws Exception {
        IntakeSession existing = persistedSession(3, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageV2ContinueRequest(
                existing.getId(), 2, "trả lời", "message_2234567890",
                "request_2234567890", List.of(), Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_STATE_VERSION_CONFLICT");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void callerCannotSelfDeclareGlobalDangerOrAbsence() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageV2ContinueRequest(
                existing.getId(), 1, "khó thở", "message_3234567890", "request_3234567890",
                List.of(), Map.of("SEVERE_BREATHING_DIFFICULTY", "PRESENT"), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_UNTRUSTED_CLINICAL_STATE");
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
            return new TriageV2WorkflowClient.WorkflowResult(
                    graphState(new AtomicReference<>(existing), "YELLOW", false),
                    "READY", "2.2.0", HASH);
        });

        // The chat asks up to three questions per round, so all three answers arrive together and
        // must cost one state version, not three.
        service.continueSession(new TriageV2ContinueRequest(
                existing.getId(), 1, "tra loi", "message_5234567890", "request_5234567890",
                List.of(new TriageV2AnswerSelection("Q_GLOBAL_DANGER", "DANGER_SEIZURE"),
                        new TriageV2AnswerSelection("Q_DIZZINESS", "DIZZINESS_NO")),
                Map.of(), Map.of()), USER);

        assertThat(strings(payload.get().get("answeredQuestionIds")))
                .containsExactly("Q_GLOBAL_DANGER", "Q_DIZZINESS");
        assertThat(objectMap(payload.get().get("signals"))).containsKeys("SEIZURE", "DIZZINESS");
    }

    @Test
    void answeringTheSameQuestionTwiceInOneTurnIsRejected() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));

        // Two answers to one question have no defined winner; silently taking the last would let a
        // client overwrite an earlier danger answer with a benign one.
        assertThatThrownBy(() -> service.continueSession(new TriageV2ContinueRequest(
                existing.getId(), 1, "tra loi", "message_6234567890", "request_6234567890",
                List.of(new TriageV2AnswerSelection("Q_GLOBAL_DANGER", "DANGER_SEIZURE"),
                        new TriageV2AnswerSelection("Q_GLOBAL_DANGER", "DANGER_NONE")),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_DUPLICATE_ANSWER");
        verify(workflow, never()).executeTurn(any());
    }

    @Test
    void aForgedQuestionIdInABatchIsRejectedBeforeTheWorkflowRuns() throws Exception {
        IntakeSession existing = persistedSession(1, "prior_request_1234", "NEEDS_MORE_INFO");
        when(repository.findForUpdateByIdAndUserId(existing.getId(), USER))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.continueSession(new TriageV2ContinueRequest(
                existing.getId(), 1, "tra loi", "message_7234567890", "request_7234567890",
                List.of(new TriageV2AnswerSelection("Q_NOT_A_REAL_QUESTION", "ANYTHING")),
                Map.of(), Map.of()), USER))
                .isInstanceOf(TriageException.class)
                .extracting("code").isEqualTo("TRIAGE_V2_UNKNOWN_QUESTION");
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

        var response = service.continueSession(new TriageV2ContinueRequest(
                existing.getId(), 1, "không rõ", "message_4234567890", "request_4234567890",
                List.of(), Map.of(), Map.of()), USER);

        assertThat(response.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(response.outcome()).isNotIn("GREEN", "OUT_OF_SCOPE");
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("FALLBACK_ONLY");
    }

    @Test
    void freeTextCannotHideInsideStructuredSignalOrMeasurement() {
        for (Map<String, Object> signals : List.of(
                Map.<String, Object>of("SEIZURE", Map.of(
                        "presence", "PRESENT", "note", "raw PII")),
                Map.<String, Object>of("PATIENT_JANE_DOE_HAS_HIV", "PRESENT"))) {
            assertThatThrownBy(() -> service.start(new TriageV2StartRequest(
                    USER, "MOTHER", Map.of(), "text", "message_5234567890",
                    "request_5234567890", Map.of("disclaimerVersion", "V2"),
                    signals, Map.of()), USER))
                    .isInstanceOf(TriageException.class)
                    .extracting("code").isEqualTo("TRIAGE_V2_UNTRUSTED_CLINICAL_STATE");
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
            state.put("citations", List.of(verifiedCitation("SOURCE_VERIFIED")));
            return new TriageV2WorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.citations()).singleElement()
                .satisfies(citation -> {
                    assertThat(citation.get("sourceStatus")).isEqualTo("SOURCE_VERIFIED");
                    assertThat(citation.get("retrievalMode")).isEqualTo("LOCAL_BM25");
                });
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
            return new TriageV2WorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.outcome()).isEqualTo("YELLOW");
        assertThat(response.citations()).isEmpty();
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("READY");
        verify(evidenceSources, never()).isApprovedDeepLink(any());
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.CITATION_REJECTED)).isEqualTo(1);
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.FALLBACK)).isZero();
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
            return new TriageV2WorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.outcome()).isEqualTo("NEEDS_MORE_INFO");
        assertThat(response.readiness().get("technicalStatus")).isEqualTo("FALLBACK_ONLY");
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.FALLBACK)).isEqualTo(1);
    }

    @Test
    void moreThanOneWhoCitationIsRejectedWithoutDowngradingOutcome() {
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
            state.put("citations", List.of(
                    verifiedCitation("SOURCE_VERIFIED"), verifiedCitation("SOURCE_VERIFIED")));
            return new TriageV2WorkflowClient.WorkflowResult(state, "READY", "2.2.0", HASH);
        });

        var response = service.start(startRequest("dau bung"), USER);

        assertThat(response.outcome()).isEqualTo("YELLOW");
        assertThat(response.citations()).isEmpty();
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.CITATION_REJECTED)).isEqualTo(1);
        assertThat(metrics.failureCount(TriageV2Metrics.Failure.FALLBACK)).isZero();
    }

    private TriageV2StartRequest startRequest(String text) {
        return new TriageV2StartRequest(USER, "MOTHER", Map.of(), text,
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
        state.put("plannedQuestionIds", List.of());
        state.put("scopeStatus", "UNKNOWN");
        state.put("pendingRiskStatuses", List.of());
        // workflowState() only trusts an envelope that carries these two: without them the state
        // is treated as a sparse Java-only fallback and deliberately not resumed.
        state.put("rawMessages", List.of());
        state.put("processedRequestIds", List.of());
        Map<String, Object> publicResponse = mapper.convertValue(
                new com.carebridge.backend.triage.dto.response.TriageV2SessionResponse(
                        id, version, "UNKNOWN", "UNKNOWN", "UNKNOWN", outcome,
                        "ASK_CLARIFYING_QUESTIONS", false, List.of(), "UNKNOWN", List.of(),
                        null, "2.2.0", HASH, List.of(), "Thông tin tham khảo",
                        Map.of("technicalStatus", "READY")), Map.class);
        Map<String, Object> envelope = Map.of(
                "contract", "triage-v2-1", "stateVersion", version,
                "lastRequestId", requestId, "v2State", state, "publicResponse", publicResponse);
        return IntakeSession.builder().id(id).userId(USER).status(IntakeStatus.NEED_MORE_INFO)
                .schemaVersion("triage-v2-1").resultJson(mapper.writeValueAsString(envelope))
                .createdAt(Instant.now()).createdBy(USER).symptoms("TRIAGE_V2_REDACTED").build();
    }
}
