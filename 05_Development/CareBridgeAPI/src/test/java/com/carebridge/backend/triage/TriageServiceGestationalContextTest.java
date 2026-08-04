package com.carebridge.backend.triage;

import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.entity.DashboardStatus;
import com.carebridge.backend.journey.service.IJourneyService;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-MATQ-IMP-001 — trusted gestational-week auto-bind (safe scope: never a question,
 * never a risk-rule input) and the new abdominalPainPattern PREGNANCY follow-up question.
 * Wiring mirrors TriageServiceHealthMemoryContextTest: 5-arg constructor + ReflectionTestUtils
 * for the optional journeyService dependency.
 */
@ExtendWith(MockitoExtension.class)
class TriageServiceGestationalContextTest {

    private static final UUID USER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private ChildTriageAiClient childTriageAiClient;
    @Mock private TriageGraphService triageGraphService;
    @Mock private IJourneyService journeyService;

    @Captor private ArgumentCaptor<Map<String, Object>> startPayloadCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Object> events = new ArrayList<>();

    private TriageService service(IJourneyService journeyServiceMock) {
        ApplicationEventPublisher recordingPublisher = events::add;
        TriageService service = new TriageService(
                intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, recordingPublisher);
        if (journeyServiceMock != null) {
            ReflectionTestUtils.setField(service, "journeyService", journeyServiceMock);
        }
        return service;
    }

    private void stubSaveThrough() {
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            var session = invocation.getArgument(0, com.carebridge.backend.triage.entity.IntakeSession.class);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            return session;
        });
    }

    private StartIntakeConversationRequest pregnancyStartRequest() {
        StartIntakeConversationRequest r = new StartIntakeConversationRequest();
        r.setStage(TriageStage.PREGNANCY);
        r.setInitialText("Tôi đau bụng");
        return r;
    }

    private static String askMoreEnvelopeJson(String questionKey) {
        return """
               {"status":"ASK_MORE","mergedIntake":{},"round":1,
                "questions":[{"questionKey":"%s","text":"Bao lâu rồi?",
                              "answerType":"TEXT","options":[]}]}""".formatted(questionKey);
    }

    @Test
    void gestationalWeek_isInjectedIntoCurrentIntake_whenJourneyHasAnActivePregnancy() {
        stubSaveThrough();
        when(journeyService.getDashboard(USER_A)).thenReturn(JourneyDashboardResponse.builder()
                .status(DashboardStatus.ACTIVE_PREGNANCY)
                .pregnancyWeek(32)
                .build());
        when(childTriageAiClient.startIntake(any())).thenReturn(askMoreEnvelopeJson("duration"));

        service(journeyService).startConversation(pregnancyStartRequest(), USER_A);

        org.mockito.Mockito.verify(childTriageAiClient).startIntake(startPayloadCaptor.capture());
        Map<?, ?> currentIntake = (Map<?, ?>) startPayloadCaptor.getValue().get("currentIntake");
        assertThat(currentIntake.get("gestationalWeeks")).isEqualTo(32);
    }

    @Test
    void gestationalWeek_isAbsent_whenDashboardHasNoComputedWeek() {
        // JourneyServiceImpl only computes pregnancyWeek for an ACTIVE PREGNANCY journey, so a
        // null week is the shape every non-pregnancy dashboard takes — the null check is the
        // status check.
        stubSaveThrough();
        when(journeyService.getDashboard(USER_A)).thenReturn(JourneyDashboardResponse.builder()
                .status(DashboardStatus.NO_JOURNEY)
                .build());
        when(childTriageAiClient.startIntake(any())).thenReturn(askMoreEnvelopeJson("duration"));

        service(journeyService).startConversation(pregnancyStartRequest(), USER_A);

        Map<String, Object> currentIntake = capturedCurrentIntake();
        assertThat(currentIntake).doesNotContainKey("gestationalWeeks");
    }

    @Test
    void gestationalWeek_isAbsent_whenJourneyLookupThrows_failOpenNeverBlocksIntake() {
        stubSaveThrough();
        when(journeyService.getDashboard(USER_A)).thenThrow(new RuntimeException("SYNTHETIC db error"));
        when(childTriageAiClient.startIntake(any())).thenReturn(askMoreEnvelopeJson("duration"));

        service(journeyService).startConversation(pregnancyStartRequest(), USER_A);

        Map<String, Object> currentIntake = capturedCurrentIntake();
        assertThat(currentIntake).doesNotContainKey("gestationalWeeks");
    }

    @Test
    void gestationalWeek_isSuppressed_whenComputedWeekIsOutOfContractRange() {
        stubSaveThrough();
        when(journeyService.getDashboard(USER_A)).thenReturn(JourneyDashboardResponse.builder()
                .status(DashboardStatus.ACTIVE_PREGNANCY)
                .pregnancyWeek(50) // Python's ChildTriageRequest bounds gestationalWeeks to 1-45
                .build());
        when(childTriageAiClient.startIntake(any())).thenReturn(askMoreEnvelopeJson("duration"));

        service(journeyService).startConversation(pregnancyStartRequest(), USER_A);

        Map<String, Object> currentIntake = capturedCurrentIntake();
        assertThat(currentIntake).doesNotContainKey("gestationalWeeks");
    }

    @Test
    void gestationalWeek_isNeverLoaded_forNonPregnancyMaternalStage() {
        stubSaveThrough();
        StartIntakeConversationRequest request = new StartIntakeConversationRequest();
        // PRECONCEPTION, not POSTPARTUM: the 5-arg test constructor has no LifecycleConsentValidator,
        // so POSTPARTUM would fail the consent gate before reaching the gestational-week load.
        request.setStage(TriageStage.PRECONCEPTION);
        request.setInitialText("Tôi đau bụng");
        when(childTriageAiClient.startIntake(any())).thenReturn(askMoreEnvelopeJson("duration"));

        service(journeyService).startConversation(request, USER_A);

        org.mockito.Mockito.verifyNoInteractions(journeyService);
    }

    @Test
    void abdominalPainPattern_questionFromPython_isNotDiscardedByStageAllowlist() {
        // Regression guard for the isQuestionAllowedForStage closed set (CB-TRIAGE-MATQ-IMP-001):
        // a valid Python ASK_MORE envelope asking abdominalPainPattern must reach the client,
        // not be silently swapped for the Java fallback envelope.
        stubSaveThrough();
        when(childTriageAiClient.startIntake(any())).thenReturn(askMoreEnvelopeJson("abdominalPainPattern"));

        var response = service(null).startConversation(pregnancyStartRequest(), USER_A);

        assertThat(response.getQuestions()).hasSize(1);
        Map<?, ?> question = (Map<?, ?>) response.getQuestions().get(0);
        assertThat(question.get("questionKey")).isEqualTo("abdominalPainPattern");
        org.mockito.Mockito.verify(triageGraphService, org.mockito.Mockito.never()).run(any(RunIntakeRequest.class));
    }

    @Test
    void gestationalWeek_clientSuppliedOnOneShotEndpoint_isOverwrittenByTheServer() {
        // The one-shot endpoint binds RunIntakeRequest straight from the request body, so a
        // forged week must be erased rather than forwarded to the AI service.
        stubSaveThrough();
        when(journeyService.getDashboard(USER_A)).thenReturn(JourneyDashboardResponse.builder()
                .status(DashboardStatus.ACTIVE_PREGNANCY)
                .pregnancyWeek(12)
                .build());
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class)))
                .thenReturn("{\"status\":\"COMPLETED\",\"riskLevel\":\"GREEN\",\"stage\":\"PREGNANCY\"}");
        RunIntakeRequest forged = RunIntakeRequest.builder()
                .stage(TriageStage.PREGNANCY)
                .symptomList(List.of("đau bụng"))
                .gestationalWeeks(40)
                .build();

        service(journeyService).runIntake(forged, USER_A);

        assertThat(forged.getGestationalWeeks()).isEqualTo(12);
    }

    @Test
    void javaFallback_keepsItsRedScreeningQuestions_whenAbdominalPainAlsoQualifies() {
        // The fallback envelope is capped at 3 questions; the descriptive abdominalPainPattern
        // question must never displace breathing/consciousness screening.
        stubSaveThrough();
        when(childTriageAiClient.startIntake(any())).thenThrow(new IllegalStateException("SYNTHETIC ai down"));
        when(triageGraphService.run(any(RunIntakeRequest.class))).thenReturn(
                com.carebridge.backend.triage.engine.ChildTriageResult.builder()
                        .status("NEED_MORE_INFO")
                        .riskLevel("GREEN")
                        .build());

        var response = service(null).startConversation(pregnancyStartRequest(), USER_A);

        List<String> keys = response.getQuestions().stream()
                .map(q -> String.valueOf(((Map<?, ?>) q).get("questionKey")))
                .toList();
        assertThat(keys).containsExactly("duration", "breathingStatus", "consciousnessStatus");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedCurrentIntake() {
        org.mockito.Mockito.verify(childTriageAiClient).startIntake(startPayloadCaptor.capture());
        return (Map<String, Object>) startPayloadCaptor.getValue().get("currentIntake");
    }
}
