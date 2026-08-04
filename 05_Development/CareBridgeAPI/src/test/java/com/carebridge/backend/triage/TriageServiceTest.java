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
import com.carebridge.backend.triage.engine.PediatricRiskRules;
import com.carebridge.backend.triage.engine.SourceRetriever;
import com.carebridge.backend.triage.engine.SymptomNormalizer;
import com.carebridge.backend.triage.engine.TriageCitation;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.ai.event.EmergencyEscalationTriggered;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.repository.IntakeSessionWriter;
import com.carebridge.backend.triage.repository.TriageSessionEvidenceWriter;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import com.carebridge.backend.triage.service.TriageFallbackMetrics;
import com.carebridge.backend.triage.service.TriageStageLegacyDefaultMetrics;
import com.carebridge.backend.triage.service.LifecycleIntakeBindingService;
import com.carebridge.backend.triage.service.LifecycleBinding;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
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
    @Mock private TriageSessionEvidenceWriter triageSessionEvidenceWriter;
    @Mock private EvidenceSourceService evidenceSourceService;
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
    void runIntake_pythonRed_shouldEscalateExactlyOnceBeforeGeneralCompletion() {
        when(childTriageAiClient.triageChild(any())).thenReturn(redJson());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeSessionResponse result = service().runIntake(
                TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        ArgumentCaptor<IntakeSession> sessionCaptor = ArgumentCaptor.forClass(IntakeSession.class);
        verify(intakeSessionRepository, atLeastOnce()).save(sessionCaptor.capture());
        IntakeSession canonical = sessionCaptor.getAllValues().getLast();
        assertThat(canonical.isEmergency()).isTrue();
        assertThat(canonical.getResultJson()).contains("\"riskLevel\":\"RED\"");
        assertThat(canonical.getSchemaVersion()).isNotBlank();
        assertThat(canonical.getContentHash()).hasSize(64);
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void runIntake_completedResult_shouldWriteOnlyValidatedEvidenceSnapshot() {
        when(childTriageAiClient.triageChild(any())).thenReturn("""
                {"status":"COMPLETED","riskLevel":"RED","stage":"INFANT",
                 "emergencyActionRequired":true,"recommendationCode":"SEEK_EMERGENCY_CARE",
                 "matchedRules":["RED_BREATHING_DISTRESS"],"responseSchemaVersion":"2.0",
                 "citations":[{"sourceId":"WHO_1","title":"WHO danger signs",
                   "organization":"WHO","url":"https://who.int/health-topics/child-health",
                   "domain":"who.int","excerpt":"danger signs","sourceVersion":"1",
                   "lastReviewed":"2026-07-10","section":"Danger signs",
                   "matchedSymptoms":[],"matchedRules":["RED_BREATHING_DISTRESS"],
                   "sourceStatus":"APPROVED","retrievedAt":"2026-07-10T00:00:00Z",
                   "retrievalMode":"LOCAL"}],
                 "claims":[{"claimId":"CLAIM-1","text":"Seek emergency care",
                   "evidenceIds":["WHO_1"]}]}
                """);
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            return session;
        });
        TriageService service = service();
        ReflectionTestUtils.setField(
                service, "triageSessionEvidenceWriter", triageSessionEvidenceWriter);

        service.runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        verify(triageSessionEvidenceWriter).writeValidated(
                any(UUID.class),
                argThat(citations -> citations.size() == 1
                        && "WHO_1".equals(citations.getFirst().get("sourceId"))),
                argThat(claims -> claims.size() == 1
                        && "CLAIM-1".equals(claims.getFirst().get("claimId"))));
    }

    @Test
    void runIntake_javaFallback_shouldPersistAndReturnOnlyApprovedLegacyCitation() {
        when(childTriageAiClient.triageChild(any())).thenThrow(new IllegalStateException("offline"));
        when(evidenceSourceService.isApprovedDeepLink(
                java.net.URI.create("https://who.int/health-topics/child-health"))).thenReturn(true);
        when(evidenceSourceService.isApprovedDeepLink(
                java.net.URI.create("https://example.com/child-health"))).thenReturn(false);
        when(triageGraphService.run(any())).thenReturn(ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel("GREEN")
                .riskColor("#22C55E")
                .summary("Mild symptoms")
                .possibleConcern("Monitor symptoms")
                .recommendedAction("Continue home monitoring")
                .emergencyActionRequired(false)
                .redFlags(java.util.List.of())
                .matchedRules(java.util.List.of("GREEN_MILD_NO_RED_FLAGS"))
                .normalizedSymptoms(java.util.List.of("cough"))
                .citations(java.util.List.of(
                        TriageCitation.builder()
                                .title("WHO child health guidance")
                                .source("WHO")
                                .url("https://who.int/health-topics/child-health")
                                .excerpt("Official child health guidance")
                                .retrievedAt("2026-07-30T00:00:00Z")
                                .build(),
                        TriageCitation.builder()
                                .title("Unapproved source")
                                .source("Unknown")
                                .url("https://example.com/child-health")
                                .excerpt("Must not pass the approved-source policy")
                                .retrievedAt("2026-07-30T00:00:00Z")
                                .build()))
                .disclaimer("AI guidance only.")
                .questions(java.util.List.of())
                .build());
        AtomicReference<IntakeSession> persisted = new AtomicReference<>();
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            persisted.set(session);
            return session;
        });
        TriageService triageService = new TriageService(
                intakeSessionRepository,
                childTriageAiClient,
                triageGraphService,
                evidenceSourceService,
                objectMapper,
                eventPublisher,
                new TriageFallbackMetrics(),
                new TriageStageLegacyDefaultMetrics());
        ReflectionTestUtils.setField(
                triageService, "triageSessionEvidenceWriter", triageSessionEvidenceWriter);

        triageService.runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        IntakeSession saved = persisted.get();
        assertThat(saved.getSchemaVersion()).isEqualTo("1.0");
        when(intakeSessionRepository.findByIdAndUserId(saved.getId(), USER_ID))
                .thenReturn(Optional.of(saved));

        TriageResultResponse response = triageService.getResult(saved.getId(), USER_ID);

        assertThat(response.getCitations()).singleElement().satisfies(citation -> {
            assertThat(citation)
                    .containsEntry("title", "WHO child health guidance")
                    .containsEntry("source", "WHO")
                    .containsEntry("url", "https://who.int/health-topics/child-health")
                    .containsEntry("sourceStatus", "APPROVED")
                    .containsEntry("retrievalMode", "LOCAL");
            assertThat(citation.get("sourceId")).isNotNull();
        });
        verify(triageSessionEvidenceWriter).writeValidated(
                eq(saved.getId()),
                argThat(citations -> citations.size() == 1
                        && "WHO".equals(citations.getFirst().get("source"))
                        && "APPROVED".equals(citations.getFirst().get("sourceStatus"))),
                argThat(java.util.List::isEmpty));
        verify(evidenceSourceService, times(2)).isApprovedDeepLink(
                java.net.URI.create("https://who.int/health-topics/child-health"));
        verify(evidenceSourceService, times(2)).isApprovedDeepLink(
                java.net.URI.create("https://example.com/child-health"));
    }

    @Test
    void runIntake_javaFallbackRed_shouldEscalateExactlyOnceBeforeGeneralCompletion() {
        when(childTriageAiClient.triageChild(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenReturn(redResult());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeSessionResponse result = service().runIntake(
                TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(result.getRiskLevel()).isEqualTo("RED");
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void runIntake_pythonUnavailablePreconceptionHeavyBleeding_shouldFallbackToRedBeforeEscalation() {
        TriageGraphService realGraph = realTriageGraphService();
        when(childTriageAiClient.triageChild(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenAnswer(invocation -> realGraph.run(invocation.getArgument(0)));
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeSessionResponse result = service().runIntake(
                com.carebridge.backend.triage.dto.request.RunIntakeRequest.builder()
                        .stage(TriageStage.PRECONCEPTION)
                        .duration("vừa xuất hiện")
                        .parentFreeText("Tôi đang chảy máu nhiều")
                        .build(),
                USER_ID);

        assertThat(result.getStage()).isEqualTo(TriageStage.PRECONCEPTION.name());
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getRiskLevel()).isEqualTo("RED");
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void startConversation_pythonUnavailablePregnancySelfHarm_shouldFallbackToRedBeforeEscalation() {
        TriageGraphService realGraph = realTriageGraphService();
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        session.setStage(TriageStage.PREGNANCY);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenAnswer(invocation -> realGraph.run(invocation.getArgument(0)));

        IntakeConversationResponse result = service().startConversation(
                StartIntakeConversationRequest.builder()
                        .stage(TriageStage.PREGNANCY)
                        .initialText("Tôi muốn tự sát ngay")
                        .currentIntake(Map.of("duration", "vừa xuất hiện"))
                        .build(),
                USER_ID);

        assertThat(result.getStage()).isEqualTo(TriageStage.PREGNANCY.name());
        assertThat(result.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(result.getTriageResult()).containsEntry("riskLevel", "RED");
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void startConversation_pythonUnavailableNegatedPregnancySigns_shouldAskMaternalFollowUpWithoutEscalation() {
        TriageGraphService realGraph = realTriageGraphService();
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        session.setStage(TriageStage.PREGNANCY);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenAnswer(invocation -> realGraph.run(invocation.getArgument(0)));

        IntakeConversationResponse result = service().startConversation(
                StartIntakeConversationRequest.builder()
                        .stage(TriageStage.PREGNANCY)
                        .initialText("Tôi không khó thở, không co giật, không ngất, không chảy máu nhiều "
                                + "và không muốn tự làm hại bản thân")
                        .currentIntake(Map.of(
                                "duration", "1 ngày",
                                "breathingStatus", "bình thường",
                                "consciousnessStatus", "tỉnh táo",
                                "seizure", false))
                        .build(),
                USER_ID);

        assertThat(result.getStage()).isEqualTo(TriageStage.PREGNANCY.name());
        assertThat(result.getStatus()).isEqualTo("ASK_MORE");
        assertThat(result.getTriageResult()).isNull();
        assertThat(result.getQuestions()).extracting(question -> String.valueOf(((Map<?, ?>) question).get("questionKey")))
                .doesNotContain("childAgeMonths", "feedingStatus", "vomiting", "diarrhea", "rash");
        verify(eventPublisher, never()).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void preconceptionAndPregnancyPythonPediatricAskMoreQuestion_shouldFallbackToMaternalQuestions() {
        for (TriageStage stage : java.util.List.of(TriageStage.PRECONCEPTION, TriageStage.PREGNANCY)) {
            reset(intakeSessionRepository, childTriageAiClient, triageGraphService, eventPublisher,
                    lifecycleConsentValidator);
            TriageGraphService realGraph = realTriageGraphService();
            IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
            session.setStage(stage);
            when(intakeSessionRepository.save(any())).thenReturn(session);
            when(childTriageAiClient.startIntake(any())).thenReturn("""
                    {"status":"ASK_MORE","mergedIntake":{"childAgeMonths":12,"feedingStatus":"good"},
                     "questions":[{"questionKey":"childAgeMonths","text":"Baby age?","answerType":"NUMBER","options":[]}],
                     "round":1}
                    """);
            when(triageGraphService.run(any())).thenAnswer(invocation -> realGraph.run(invocation.getArgument(0)));

            IntakeConversationResponse result = service().startConversation(
                    StartIntakeConversationRequest.builder()
                            .stage(stage)
                            .initialText("Tôi cần hỗ trợ sức khỏe")
                            .currentIntake(Map.of("childAgeMonths", 12, "feedingStatus", "good"))
                            .build(),
                    USER_ID);

            assertThat(result.getStatus()).as(stage.name()).isEqualTo("ASK_MORE");
            assertThat(result.getQuestions()).as(stage.name())
                    .extracting(question -> String.valueOf(((Map<?, ?>) question).get("questionKey")))
                    .doesNotContain("childAgeMonths", "feedingStatus", "vomiting", "diarrhea", "rash");
            assertThat(result.getQuestions()).as(stage.name())
                    .extracting(question -> String.valueOf(((Map<?, ?>) question).get("text")))
                    .allSatisfy(text -> assertThat(text).doesNotContain("trẻ", "bé", "bú", "child", "baby"));
            assertThat(result.getMergedIntake()).as(stage.name())
                    .doesNotContainKeys("babyProfileId", "childAgeMonths", "feedingStatus", "vomiting", "diarrhea", "rash");
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
            verify(childTriageAiClient).startIntake(requestCaptor.capture());
            Map<?, ?> outboundIntake = (Map<?, ?>) requestCaptor.getValue().get("currentIntake");
            assertThat(java.util.List.of(
                    "babyProfileId", "childAgeMonths", "feedingStatus", "vomiting", "diarrhea", "rash"))
                    .noneMatch(outboundIntake::containsKey);
            verify(triageGraphService).run(any());
        }
    }

    @Test
    void pregnancyJavaFallbackAtQuestionLimit_shouldReturnMaternalCautiousYellowCopy() {
        TriageGraphService realGraph = realTriageGraphService();
        IntakeSession session = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","stage":"PREGNANCY",
                 "mergedIntake":{"stage":"PREGNANCY","parentFreeText":"Tôi chóng mặt"},
                 "questions":[{"questionKey":"duration","text":"Dấu hiệu kéo dài bao lâu?","answerType":"TEXT","options":[]}],
                 "round":3}
                """);
        session.setStage(TriageStage.PREGNANCY);
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID))
                .thenReturn(Optional.of(session));
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(childTriageAiClient.continueIntake(any())).thenThrow(new IllegalStateException("offline"));
        when(triageGraphService.run(any())).thenAnswer(invocation -> realGraph.run(invocation.getArgument(0)));

        IntakeConversationResponse result = service().continueConversation(
                ContinueIntakeConversationRequest.builder()
                        .intakeSessionId(session.getId().toString())
                        .newAnswers(Map.of("duration", "1 ngày"))
                        .build(),
                USER_ID);

        assertThat(result.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(result.getTriageResult())
                .containsEntry("riskLevel", "YELLOW")
                .containsEntry("matchedRules", java.util.List.of("YELLOW_INCOMPLETE_INFORMATION"));
        assertThat(java.util.List.of(
                        "summary", "possibleConcern", "recommendedAction", "warning", "disclaimer"))
                .allSatisfy(field -> assertThat(String.valueOf(result.getTriageResult().get(field)))
                        .doesNotContain("trẻ", "bé", "bú", "child", "baby"));
        verify(eventPublisher, never()).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void runIntake_green_shouldNeverEscalateEmergency() {
        when(childTriageAiClient.triageChild(any())).thenReturn(greenJson());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().runIntake(TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        verify(eventPublisher, never()).publishEvent(any(EmergencyEscalationTriggered.class));
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
            assertThat(listAppender.list)
                    .noneMatch(event -> event.getFormattedMessage().contains(USER_ID.toString()))
                    .noneMatch(event -> event.getFormattedMessage().contains(saved.getId().toString()));
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
                {"status":"ASK_MORE","assistantMessage":"Need duration","mergedIntake":{},"questions":[{"questionKey":"duration","text":"How long?","answerType":"TEXT","options":[]}],"round":1}
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
    void startConversation_babyReplayWithoutTypedBinding_shouldRejectConflict() {
        String clientRequestId = "baby-replay-missing-binding";
        UUID babyId = UUID.randomUUID();
        IntakeSession existing = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","intakeSessionId":"00000000-0000-0000-0000-000000000001",
                 "mergedIntake":{},"questions":[{"questionKey":"duration","text":"How long?","answerType":"TEXT","options":[]}],"round":1}
                """);
        existing.setClientRequestId(clientRequestId);
        existing.setJourneyId(null);
        existing.setBabyProfileId(babyId);
        existing.setStage(TriageStage.INFANT);
        existing.setOriginDashboard(OriginDashboard.BABY_PROFILE);
        existing.setOriginReferenceId(babyId);
        when(intakeSessionRepository.findByUserIdAndClientRequestId(USER_ID, clientRequestId))
                .thenReturn(Optional.of(existing));
        LifecycleIntakeBindingService bindingService = mock(LifecycleIntakeBindingService.class);
        TriageException conflict = new TriageException(
                HttpStatus.CONFLICT, "TRIAGE-016", "Intake context conflict");
        doThrow(conflict).when(bindingService).validateReplay(existing, null);
        TriageService triageService = service();
        ReflectionTestUtils.setField(triageService, "lifecycleBindingService", bindingService);
        StartIntakeConversationRequest request = StartIntakeConversationRequest.builder()
                .initialText("be sot")
                .clientRequestId(clientRequestId)
                .stage(TriageStage.INFANT)
                .babyProfileId(babyId)
                .build();

        assertThatThrownBy(() -> triageService.startConversation(request, USER_ID))
                .isSameAs(conflict);

        verify(bindingService).validateReplay(existing, null);
        verifyNoInteractions(childTriageAiClient, eventPublisher);
    }

    @Test
    void startConversation_databaseRaceLoser_shouldReloadWinnerAndReplayWithoutSideEffects() {
        String clientRequestId = "idempotency-race-1234";
        IntakeSession winner = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","intakeSessionId":"00000000-0000-0000-0000-000000000001",
                 "mergedIntake":{},"questions":[{"questionKey":"duration","text":"How long?","answerType":"TEXT","options":[]}],"round":1}
                """);
        winner.setClientRequestId(clientRequestId);
        when(intakeSessionRepository.findByUserIdAndClientRequestId(USER_ID, clientRequestId))
                .thenReturn(Optional.empty(), Optional.of(winner));
        IntakeSessionWriter writer = mock(IntakeSessionWriter.class);
        when(writer.insertConversationIfAbsent(any()))
                .thenReturn(new IntakeSessionWriter.InsertResult(false));
        TriageService triageService = service();
        ReflectionTestUtils.setField(triageService, "intakeSessionWriter", writer);

        IntakeConversationResponse response = triageService.startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("be sot")
                        .clientRequestId(clientRequestId)
                        .build(), USER_ID);

        assertThat(response.getIntakeSessionId()).isEqualTo(winner.getId().toString());
        verify(writer).insertConversationIfAbsent(any(IntakeSession.class));
        verify(intakeSessionRepository, never()).save(any());
        verifyNoInteractions(childTriageAiClient, eventPublisher);
    }

    @Test
    void startConversation_unrelatedInsertConstraintFailure_shouldPropagate() {
        String clientRequestId = "idempotency-error-1234";
        when(intakeSessionRepository.findByUserIdAndClientRequestId(USER_ID, clientRequestId))
                .thenReturn(Optional.empty());
        IntakeSessionWriter writer = mock(IntakeSessionWriter.class);
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("unrelated check constraint");
        when(writer.insertConversationIfAbsent(any())).thenThrow(failure);
        TriageService triageService = service();
        ReflectionTestUtils.setField(triageService, "intakeSessionWriter", writer);

        assertThatThrownBy(() -> triageService.startConversation(
                StartIntakeConversationRequest.builder()
                        .initialText("be sot")
                        .clientRequestId(clientRequestId)
                        .build(), USER_ID))
                .isSameAs(failure);

        verify(intakeSessionRepository, times(1))
                .findByUserIdAndClientRequestId(USER_ID, clientRequestId);
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
    void continueConversation_terminalBabyResult_renewsContinuationWithoutJourneyBeforeSaveAndProjectionEvent() {
        IntakeSession session = conversationSession(IntakeStatus.NEED_MORE_INFO, """
                {"status":"ASK_MORE","intakeSessionId":"00000000-0000-0000-0000-000000000001",
                 "mergedIntake":{},"questions":[{"questionKey":"duration","text":"Age?","answerType":"TEXT","options":[]}],"round":1}
                """);
        UUID babyId = UUID.randomUUID();
        session.setJourneyId(null);
        session.setBabyProfileId(babyId);
        session.setOriginDashboard(OriginDashboard.BABY_PROFILE);
        session.setOriginReferenceId(babyId);
        UUID continuationToken = UUID.randomUUID();
        Instant continuationExpiresAt = Instant.now().plusSeconds(600);
        session.setContinuationToken(continuationToken);
        session.setContinuationExpiresAt(continuationExpiresAt);
        when(intakeSessionRepository.findForUpdateByIdAndUserId(session.getId(), USER_ID))
                .thenReturn(Optional.of(session));
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(childTriageAiClient.continueIntake(any())).thenReturn("""
                {"status":"TRIAGE_COMPLETE","mergedIntake":{},"questions":[],"round":2,
                 "triageResult":{"riskLevel":"GREEN","matchedRules":["GREEN_MILD_NO_RED_FLAGS"],
                 "recommendationCode":"MONITOR_AT_HOME","graphVersion":"python-1",
                 "ruleSetVersion":"rules-1","ontologyVersion":"ontology-1","responseSchemaVersion":"2.0"}}
                """);
        LifecycleIntakeBindingService bindingService = mock(LifecycleIntakeBindingService.class);
        TriageService triageService = service();
        ReflectionTestUtils.setField(triageService, "lifecycleBindingService", bindingService);

        IntakeConversationResponse response = triageService.continueConversation(
                ContinueIntakeConversationRequest.builder()
                .intakeSessionId(session.getId().toString())
                .newAnswers(Map.of("duration", "one day"))
                .build(), USER_ID);

        assertThat(response.getJourneyId()).isNull();
        assertThat(response.getOriginDashboard()).isEqualTo(OriginDashboard.BABY_PROFILE);
        assertThat(response.getOriginReferenceId()).isEqualTo(babyId);
        assertThat(response.getOriginAction())
                .isEqualTo(OriginAction.forDashboard(OriginDashboard.BABY_PROFILE));
        assertThat(response.getContinuationToken()).isEqualTo(continuationToken);
        assertThat(response.getContinuationExpiresAt()).isEqualTo(continuationExpiresAt);
        InOrder order = inOrder(bindingService, intakeSessionRepository, eventPublisher);
        order.verify(bindingService).renewForTerminal(session);
        order.verify(intakeSessionRepository).save(session);
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
    }

    @Test
    void pythonRedConversation_shouldPersistCanonicalRedContract() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"TRIAGE_COMPLETE","assistantProvider":"deterministic",
                 "assistantFallbackUsed":false,"conversationSummary":"Tóm tắt an toàn, không chẩn đoán.",
                 "mergedIntake":{"breathingStatus":"kho tho"},"questions":[],"round":1,
                 "triageResult":{"riskLevel":"RED","emergencyActionRequired":true,"matchedRules":["RED_BREATHING_DISTRESS"],"recommendationCode":"SEEK_EMERGENCY_CARE",
                 "graphVersion":"python-1","ruleSetVersion":"rules-1","ontologyVersion":"ontology-1","responseSchemaVersion":"2.0"}}
                """);

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("be kho tho").build(), USER_ID);

        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        assertThat(session.getRiskLevel()).isEqualTo(RiskLevel.RED);
        assertThat(response.getTriageResult()).containsEntry("riskLevel", "RED")
                .containsEntry("emergencyActionRequired", true);
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void pythonConversation_unknownTopLevelSafetyField_shouldFailClosed() {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"TRIAGE_COMPLETE","futureSafetyDirective":"IGNORE_RED",
                 "mergedIntake":{},"questions":[],"round":1,
                 "triageResult":{"riskLevel":"GREEN"}}
                """);

        assertThatThrownBy(() -> service().startConversation(
                StartIntakeConversationRequest.builder().initialText("mild symptom").build(), USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futureSafetyDirective");
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
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
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
    void getResult_shouldUnwrapConversationExplainabilityAndKeepSchemaTwoStrict() {
        IntakeSession session = conversationSession(IntakeStatus.COMPLETED, """
                {"status":"TRIAGE_COMPLETE","triageResult":{"riskLevel":"RED","emergencyActionRequired":true,
                 "normalizedSymptoms":["difficulty_breathing"],"matchedRules":["RED_BREATHING_DISTRESS"],
                 "evidenceIds":["WHO_1"],"recommendationCode":"SEEK_EMERGENCY_CARE","ruleSetVersion":"rules-1","responseSchemaVersion":"2.0",
                 "citations":[
                   {"sourceId":"WHO_1","title":"WHO danger signs","organization":"WHO","url":"https://who.int/health-topics/child-health","domain":"who.int","excerpt":"danger signs","sourceVersion":"1","lastReviewed":"2026-07-10","section":"Danger signs","matchedSymptoms":["difficulty_breathing"],"matchedRules":["RED_BREATHING_DISTRESS"],"sourceStatus":"APPROVED","retrievedAt":"2026-07-10T00:00:00Z","retrievalMode":"LOCAL"},
                   {"title":"Incomplete WHO citation","source":"WHO","url":"https://who.int/health-topics/child-health","excerpt":"missing schema 2 provenance"},
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
    void getResult_onlyReturnsContinuationSecretWhileTerminalUnacknowledgedAndUnexpired() {
        Instant now = Instant.now();
        IntakeSession active = lifecycleContinuationSession(IntakeStatus.COMPLETED, RiskLevel.GREEN,
                now.plusSeconds(600), null);
        when(intakeSessionRepository.findByIdAndUserId(active.getId(), USER_ID))
                .thenReturn(Optional.of(active));

        TriageResultResponse activeResponse = service().getResult(active.getId(), USER_ID);
        assertThat(activeResponse.getContinuationToken()).isEqualTo(active.getContinuationToken());
        assertThat(activeResponse.getContinuationExpiresAt()).isEqualTo(active.getContinuationExpiresAt());

        for (IntakeSession retired : java.util.List.of(
                lifecycleContinuationSession(IntakeStatus.NEED_MORE_INFO, null,
                        now.plusSeconds(600), null),
                lifecycleContinuationSession(IntakeStatus.COMPLETED, null,
                        now.plusSeconds(600), null),
                lifecycleContinuationSession(IntakeStatus.COMPLETED, RiskLevel.GREEN,
                        now.minusSeconds(1), null),
                lifecycleContinuationSession(IntakeStatus.COMPLETED, RiskLevel.GREEN,
                        now.plusSeconds(600), now.minusSeconds(1)))) {
            when(intakeSessionRepository.findByIdAndUserId(retired.getId(), USER_ID))
                    .thenReturn(Optional.of(retired));

            TriageResultResponse response = service().getResult(retired.getId(), USER_ID);

            assertThat(response.getContinuationToken()).isNull();
            assertThat(response.getContinuationExpiresAt()).isNull();
        }
    }

    @Test
    void listSessions_neverReturnsRetiredContinuationSecrets() {
        Instant now = Instant.now();
        IntakeSession active = lifecycleContinuationSession(IntakeStatus.COMPLETED, RiskLevel.YELLOW,
                now.plusSeconds(600), null);
        IntakeSession acknowledged = lifecycleContinuationSession(
                IntakeStatus.COMPLETED, RiskLevel.GREEN,
                now.plusSeconds(600), now.minusSeconds(1));
        IntakeSession expired = lifecycleContinuationSession(
                IntakeStatus.COMPLETED, RiskLevel.RED,
                now.minusSeconds(1), null);
        IntakeSession preterminal = lifecycleContinuationSession(
                IntakeStatus.PROCESSING, null,
                now.plusSeconds(600), null);
        when(intakeSessionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(java.util.List.of(active, acknowledged, expired, preterminal));

        java.util.List<IntakeSessionResponse> responses = service().listSessions(USER_ID);

        assertThat(responses.getFirst().getContinuationToken()).isEqualTo(active.getContinuationToken());
        assertThat(responses.getFirst().getContinuationExpiresAt()).isEqualTo(active.getContinuationExpiresAt());
        assertThat(responses.subList(1, responses.size()))
                .allSatisfy(response -> {
                    assertThat(response.getContinuationToken()).isNull();
                    assertThat(response.getContinuationExpiresAt()).isNull();
                });
    }

    @Test
    void lifecycleContinuationCreatedMetric_isRecordedOnlyForNewPersistedSessionNotReplay() {
        LifecycleIntakeBindingService bindingService = mock(LifecycleIntakeBindingService.class);
        UUID journeyId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        LifecycleBinding binding = new LifecycleBinding(
                journeyId, OriginDashboard.MOTHER_JOURNEY, originId,
                TriageStage.INFANT, UUID.randomUUID(), Instant.now().plusSeconds(600));
        when(bindingService.bindForStart(any(), eq(TriageStage.INFANT), eq(USER_ID)))
                .thenReturn(binding);
        AtomicReference<IntakeSession> persisted = new AtomicReference<>();
        when(intakeSessionRepository.findByUserIdAndClientRequestId(
                USER_ID, "story67-created-once")).thenReturn(Optional.empty());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            persisted.set(session);
            return session;
        });
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","mergedIntake":{},
                 "questions":[{"questionKey":"duration","text":"Bao lau?","answerType":"TEXT","options":[]}],
                 "round":1}
                """);
        TriageService triageService = service();
        ReflectionTestUtils.setField(triageService, "lifecycleBindingService", bindingService);
        StartIntakeConversationRequest request = StartIntakeConversationRequest.builder()
                .clientRequestId("story67-created-once")
                .stage(TriageStage.INFANT)
                .journeyId(journeyId)
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .originReferenceId(originId)
                .initialText("mild symptom")
                .build();

        triageService.startConversation(request, USER_ID);
        when(intakeSessionRepository.findByUserIdAndClientRequestId(
                USER_ID, "story67-created-once")).thenReturn(Optional.of(persisted.get()));
        triageService.startConversation(request, USER_ID);

        verify(bindingService, times(1)).recordCreated();
        verify(bindingService, times(1)).validateReplay(persisted.get(), binding);
        verify(childTriageAiClient, times(1)).startIntake(any());
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

    @Test
    void oneShotRedWithNeedMoreStatus_shouldFallbackThenPersistTerminalRedAndEscalate() {
        when(childTriageAiClient.triageChild(any())).thenReturn("""
                {"status":"NEED_MORE_INFO","riskLevel":"RED","emergencyActionRequired":true,
                 "matchedRules":["RED_BREATHING_DISTRESS"],"recommendationCode":"SEEK_EMERGENCY_CARE"}
                """);
        when(triageGraphService.run(any())).thenReturn(redResult());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeSessionResponse response = service().runIntake(
                TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getRiskLevel()).isEqualTo("RED");
        verify(triageGraphService).run(any());
        InOrder order = inOrder(eventPublisher);
        order.verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
        order.verify(eventPublisher).publishEvent(any(IntakeSessionCompleted.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void oneShotExplicitStageMismatch_shouldFallbackAndPersistCanonicalStage() {
        when(childTriageAiClient.triageChild(any())).thenReturn("""
                {"status":"COMPLETED","stage":"POSTPARTUM","riskLevel":"GREEN",
                 "emergencyActionRequired":false,"matchedRules":["GREEN_MILD_NO_RED_FLAGS"]}
                """);
        when(triageGraphService.run(any())).thenReturn(greenResult());
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IntakeSessionResponse response = service().runIntake(
                TriageTestFactory.makeRunIntakeRequest(), USER_ID);

        assertThat(response.getStage()).isEqualTo(TriageStage.INFANT.name());
        ArgumentCaptor<IntakeSession> captor = ArgumentCaptor.forClass(IntakeSession.class);
        verify(intakeSessionRepository, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getRawAiResponse())
                .contains("\"stage\":\"INFANT\"");
        verify(triageGraphService).run(any());
    }

    @Test
    void askMoreEnvelopeCarryingRedResult_shouldFallbackToTerminalRedAndEscalate() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"ASK_MORE","mergedIntake":{},
                 "questions":[{"questionKey":"duration","text":"How long?","answerType":"TEXT","options":[]}],
                 "round":1,"triageResult":{"status":"NEED_MORE_INFO","riskLevel":"RED",
                 "emergencyActionRequired":true,"matchedRules":["RED_BREATHING_DISTRESS"],
                 "recommendationCode":"SEEK_EMERGENCY_CARE"}}
                """);
        when(triageGraphService.run(any())).thenReturn(redResult());

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("danger sign").build(), USER_ID);

        assertThat(response.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(response.getStage()).isEqualTo(TriageStage.INFANT.name());
        assertThat(response.getMergedIntake()).containsEntry("stage", TriageStage.INFANT.name());
        assertThat(response.getTriageResult())
                .containsEntry("stage", TriageStage.INFANT.name())
                .containsEntry("riskLevel", "RED");
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void completedRedResultWithNonterminalNestedStatus_shouldFallbackBeforePersistence() {
        IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
        when(intakeSessionRepository.save(any())).thenReturn(session);
        when(childTriageAiClient.startIntake(any())).thenReturn("""
                {"status":"TRIAGE_COMPLETE","mergedIntake":{},"questions":[],"round":1,
                 "triageResult":{"status":"NEED_MORE_INFO","riskLevel":"RED",
                 "emergencyActionRequired":true,"matchedRules":["RED_BREATHING_DISTRESS"],
                 "recommendationCode":"SEEK_EMERGENCY_CARE"}}
                """);
        when(triageGraphService.run(any())).thenReturn(redResult());

        IntakeConversationResponse response = service().startConversation(
                StartIntakeConversationRequest.builder().initialText("danger sign").build(), USER_ID);

        assertThat(response.getStatus()).isEqualTo("TRIAGE_COMPLETE");
        assertThat(response.getTriageResult()).containsEntry("riskLevel", "RED");
        assertThat(session.getStatus()).isEqualTo(IntakeStatus.COMPLETED);
        verify(triageGraphService).run(any());
        verify(eventPublisher).publishEvent(any(EmergencyEscalationTriggered.class));
    }

    @Test
    void explicitStageMismatchAtEveryConversationBoundary_shouldFallbackAndCanonicalizeAllStages() {
        for (String unsafeResponse : java.util.List.of(
                """
                {"status":"TRIAGE_COMPLETE","stage":"POSTPARTUM","mergedIntake":{},
                 "triageResult":{"riskLevel":"GREEN"}}
                """,
                """
                {"status":"TRIAGE_COMPLETE","mergedIntake":{"stage":"POSTPARTUM"},
                 "triageResult":{"riskLevel":"GREEN"}}
                """,
                """
                {"status":"TRIAGE_COMPLETE","mergedIntake":{},
                 "triageResult":{"stage":"POSTPARTUM","riskLevel":"GREEN"}}
                """)) {
            IntakeSession session = conversationSession(IntakeStatus.PROCESSING, null);
            when(intakeSessionRepository.save(any())).thenReturn(session);
            when(childTriageAiClient.startIntake(any())).thenReturn(unsafeResponse);
            when(triageGraphService.run(any())).thenReturn(greenResult());

            IntakeConversationResponse response = service().startConversation(
                    StartIntakeConversationRequest.builder()
                            .stage(TriageStage.INFANT)
                            .initialText("mild symptom")
                            .build(), USER_ID);

            assertThat(response.getStage()).isEqualTo(TriageStage.INFANT.name());
            assertThat(response.getMergedIntake()).containsEntry("stage", TriageStage.INFANT.name());
            assertThat(response.getTriageResult()).containsEntry("stage", TriageStage.INFANT.name());
        }
        verify(triageGraphService, times(3)).run(any());
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

    private TriageGraphService realTriageGraphService() {
        return new TriageGraphService(
                new SymptomNormalizer(),
                new SourceRetriever(),
                new PediatricRiskRules());
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
            session.setStage(TriageStage.INFANT);
            session.setRawAiResponse(rawResponse);
            session.setSymptoms("CONVERSATION_INTAKE");
            session.setCreatedAt(Instant.parse("2026-07-13T00:00:00Z"));
        });
    }

    private IntakeSession lifecycleContinuationSession(
            IntakeStatus status, RiskLevel riskLevel, Instant expiresAt, Instant acknowledgedAt) {
        IntakeSession session = conversationSession(status,
                "{\"riskLevel\":\"GREEN\",\"citations\":[]}");
        session.setRiskLevel(riskLevel);
        session.setJourneyId(UUID.randomUUID());
        session.setOriginDashboard(OriginDashboard.MOTHER_JOURNEY);
        session.setOriginReferenceId(UUID.randomUUID());
        session.setContinuationToken(UUID.randomUUID());
        session.setContinuationExpiresAt(expiresAt);
        session.setContinuationAcknowledgedAt(acknowledgedAt);
        return session;
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

    private String redJson() {
        return """
                {
                  "riskLevel": "RED",
                  "riskColor": "#EF4444",
                  "summary": "Immediate danger sign",
                  "possibleConcern": "Difficulty breathing",
                  "recommendedAction": "Seek emergency care",
                  "emergencyActionRequired": true,
                  "redFlags": ["Difficulty breathing"],
                  "matchedRules": ["RED_BREATHING_DISTRESS"],
                  "citations": [],
                  "questions": [],
                  "recommendationCode": "SEEK_EMERGENCY_CARE",
                  "disclaimer": "Risk classification only."
                }
                """;
    }
}
