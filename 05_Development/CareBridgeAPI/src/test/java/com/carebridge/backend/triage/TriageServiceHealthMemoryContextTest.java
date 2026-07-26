package com.carebridge.backend.triage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.engine.ChildTriageResult;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.policy.HealthMemorySummaryPolicy;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.HealthMemoryService;
import com.carebridge.backend.triage.service.impl.HealthMemoryServiceImpl;
import com.carebridge.backend.triage.service.impl.TriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.BABY_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.USER_A;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.USER_B;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeActiveMemory;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeAiAskMoreEnvelopeJson;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeAiOneShotGreenJson;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeContextItem;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeProperties;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeRunIntakeRequest;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeStartConversationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — injection workflow (THMC-TC-06/09/11/13/14 + TC-15 service layer).
 * Mirrors the TriageServicePreScreenTest wiring: 5-arg constructor + ReflectionTestUtils
 * for the optional healthMemoryService dependency.
 */
@ExtendWith(MockitoExtension.class)
class TriageServiceHealthMemoryContextTest {

    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private ChildTriageAiClient childTriageAiClient;
    @Mock private TriageGraphService triageGraphService;
    @Mock private HealthMemoryService healthMemoryService;
    @Mock private HealthMemoryEntryRepository memoryRepository;

    @Captor private ArgumentCaptor<List<HealthMemoryContextItem>> contextCaptor;
    @Captor private ArgumentCaptor<List<HealthMemoryContextItem>> fallbackContextCaptor;
    @Captor private ArgumentCaptor<Map<String, Object>> startPayloadCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Object> events = new ArrayList<>();

    private TriageService service(HealthMemoryService memoryService) {
        ApplicationEventPublisher recordingPublisher = events::add;
        TriageService service = new TriageService(
                intakeSessionRepository, childTriageAiClient, triageGraphService,
                objectMapper, recordingPublisher);
        ReflectionTestUtils.setField(service, "healthMemoryService", memoryService);
        return service;
    }

    private void stubSaveThrough() {
        when(intakeSessionRepository.save(any())).thenAnswer(invocation -> {
            IntakeSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(UUID.randomUUID());
            }
            return session;
        });
    }

    @Test
    void thmcTc06_runIntake_injectsActiveMemoriesIntoOutboundAiPayload() {
        // Oracle: Roadmap III.1(b) / US-THMC-002 / TDS §8.1 ChildTriageAiClient v2.0
        stubSaveThrough();
        when(healthMemoryService.loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1))
                .thenReturn(List.of(makeContextItem()));
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class), anyList()))
                .thenReturn(makeAiOneShotGreenJson());

        service(healthMemoryService).runIntake(makeRunIntakeRequest(), USER_A);

        verify(childTriageAiClient, times(1))
                .triageChild(any(RunIntakeRequest.class), contextCaptor.capture());
        assertThat(contextCaptor.getValue()).hasSize(1);
        assertThat(contextCaptor.getValue().get(0).summaryText())
                .isEqualTo("SYNTHETIC prior triage: risk YELLOW; fever, cough");
        assertThat(contextCaptor.getValue().get(0).relatedStage()).isEqualTo("INFANT");
        // Subject keys from the resolved request; userId from the auth parameter only
        verify(healthMemoryService, times(1))
                .loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1);
    }

    @Test
    void thmcTc09_ownership_anotherUsersMemoriesNeverInjected() {
        // Oracle: repository owner filter (:16/:26) / BR-THMC-002 / TDS §16 Own-only
        // Attack simulation: backing store holds ONLY a foreign user's memory; the REAL
        // HealthMemoryServiceImpl is wired so the owner-key path is exercised (CWE-639).
        stubSaveThrough();
        when(memoryRepository.findActivePediatric(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    UUID ownerParam = invocation.getArgument(0);
                    List<com.carebridge.backend.triage.entity.HealthMemoryEntry> store =
                            List.of(makeActiveMemory(e -> {
                                e.setUserId(USER_B);
                                e.setSummaryText("SYNTHETIC FOREIGN_USER_B_SUMMARY");
                            }));
                    return store.stream()
                            .filter(entry -> entry.getUserId().equals(ownerParam))
                            .toList();
                });
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class)))
                .thenReturn(makeAiOneShotGreenJson());
        HealthMemoryServiceImpl realMemoryService = new HealthMemoryServiceImpl(
                memoryRepository, intakeSessionRepository,
                new HealthMemorySummaryPolicy(objectMapper), makeProperties());

        service(realMemoryService).runIntake(makeRunIntakeRequest(), USER_A);

        ArgumentCaptor<UUID> ownerCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(memoryRepository, times(1)).findActivePediatric(
                ownerCaptor.capture(), eq(BABY_1), eq(TriageStage.INFANT), any(Instant.class));
        assertThat(ownerCaptor.getValue()).isEqualTo(USER_A); // never USER_B, never from body
        // Deviation D1 (2026-07-26): empty server-loaded context keeps the LEGACY one-arg AI
        // contract (wire-identical to an omitted additive field); the two-arg overload — the
        // only channel that could carry foreign summaries — is never touched.
        verify(childTriageAiClient, times(1)).triageChild(any(RunIntakeRequest.class));
        verify(childTriageAiClient, never())
                .triageChild(any(RunIntakeRequest.class), anyList());
    }

    @Test
    void thmcTc11_javaFallbackReceivesSameContextWhenAiClientFails() {
        // Oracle: Roadmap III.1(b) "+ Java fallback" / TDS §8.1 TriageGraphService v2.0
        stubSaveThrough();
        when(healthMemoryService.loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1))
                .thenReturn(List.of(makeContextItem()));
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class), anyList()))
                .thenThrow(new IllegalStateException("AI triage service unavailable",
                        new IOException())); // FX-THMC-011
        when(triageGraphService.run(any(RunIntakeRequest.class), anyList()))
                .thenReturn(deterministicGreenResult());

        IntakeSessionResponse response =
                service(healthMemoryService).runIntake(makeRunIntakeRequest(), USER_A);

        verify(childTriageAiClient).triageChild(any(RunIntakeRequest.class), contextCaptor.capture());
        verify(triageGraphService, times(1))
                .run(any(RunIntakeRequest.class), fallbackContextCaptor.capture());
        // No context loss on degradation: identical items, identical order
        assertThat(fallbackContextCaptor.getValue()).isEqualTo(contextCaptor.getValue());
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void thmcTc13_memoryReadFailure_neverBlocksIntake_failOpenWithWarn() {
        // Oracle: BR-THMC-004 / ADR-THMC-003 Option B / TDS §4.1 (0 memory-caused failures)
        stubSaveThrough();
        when(healthMemoryService.loadContextForIntake(any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("SYNTHETIC"));
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class)))
                .thenReturn(makeAiOneShotGreenJson());
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger triageLogger = (Logger) LoggerFactory.getLogger(TriageService.class);
        triageLogger.addAppender(appender);
        IntakeSessionResponse response;
        try {
            response = assertDoesNotThrow(
                    () -> service(healthMemoryService).runIntake(makeRunIntakeRequest(), USER_A));
        } finally {
            triageLogger.detachAppender(appender);
        }

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        // Context degraded to empty — intake proceeds on the legacy one-arg contract
        // (Deviation D1: empty context == omitted additive field == pre-feature call shape)
        verify(childTriageAiClient, times(1)).triageChild(any(RunIntakeRequest.class));
        // Positive control — the fail-open wrapper exists and was exercised
        verify(healthMemoryService, times(1)).loadContextForIntake(any(), any(), any());
        List<ILoggingEvent> warns = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .filter(event -> event.getFormattedMessage().toLowerCase()
                        .contains("health memory context unavailable"))
                .toList();
        assertThat(warns).isNotEmpty();
        // No PII / identifiers beyond correlation-safe fields in the WARN line
        assertThat(warns).noneMatch(event ->
                event.getFormattedMessage().contains(USER_A.toString())
                        || event.getFormattedMessage().contains("SYNTHETIC prior triage"));
    }

    @Test
    void thmcTc14_startConversation_putsHealthContextIntoCanonicalStartPayload() {
        // Oracle: Roadmap III.1(b) / TriageService canonicalRequest map / TDS §9.2 start contract
        stubSaveThrough();
        when(healthMemoryService.loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1))
                .thenReturn(List.of(makeContextItem()));
        when(childTriageAiClient.startIntake(any())).thenReturn(makeAiAskMoreEnvelopeJson());

        service(healthMemoryService).startConversation(makeStartConversationRequest(), USER_A);

        verify(childTriageAiClient, times(1)).startIntake(startPayloadCaptor.capture());
        Map<String, Object> payload = startPayloadCaptor.getValue();
        // Existing canonical keys undisturbed (no regression on the map contract)
        assertThat(payload).containsKeys("initialText", "currentIntake", "intakeSessionId", "stage");
        assertThat(payload).containsKey("healthContext");
        List<?> context = (List<?>) payload.get("healthContext");
        assertThat(context).hasSize(1);
        Map<?, ?> item = (Map<?, ?>) context.get(0);
        assertThat(item.get("summaryText"))
                .isEqualTo("SYNTHETIC prior triage: risk YELLOW; fever, cough");
        assertThat(item.get("relatedStage")).isEqualTo("INFANT");
    }

    @Test
    void thmcTc15_serviceLayer_outboundContextIsExactlyTheServerLoadedList() {
        // Oracle: BR-THMC-006 / TDS §17 C4 — RunIntakeRequest has no healthContext field
        // (guard by design); the outbound list is exactly the server-loaded one (empty here).
        stubSaveThrough();
        when(healthMemoryService.loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1))
                .thenReturn(List.of());
        when(childTriageAiClient.triageChild(any(RunIntakeRequest.class)))
                .thenReturn(makeAiOneShotGreenJson());

        service(healthMemoryService).runIntake(makeRunIntakeRequest(), USER_A);

        // Server-loaded list is empty ⇒ legacy one-arg call (Deviation D1); the two-arg
        // context channel is never invoked, so nothing client-supplied can ride along.
        verify(childTriageAiClient, times(1)).triageChild(any(RunIntakeRequest.class));
        verify(childTriageAiClient, never())
                .triageChild(any(RunIntakeRequest.class), anyList());
        // Design guard: the public DTO must never gain a healthContext property (C4)
        assertThat(java.util.Arrays.stream(RunIntakeRequest.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("healthContext");
        verify(healthMemoryService, times(1))
                .loadContextForIntake(USER_A, TriageStage.INFANT, BABY_1);
    }

    private ChildTriageResult deterministicGreenResult() {
        return ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel("GREEN")
                .riskColor("#22C55E")
                .summary("SYNTHETIC no danger signs")
                .possibleConcern("SYNTHETIC mild symptoms")
                .recommendedAction("SYNTHETIC monitor at home")
                .emergencyActionRequired(false)
                .redFlags(List.of())
                .matchedRules(List.of())
                .normalizedSymptoms(List.of())
                .citations(List.of())
                .disclaimer("SYNTHETIC disclaimer")
                .questions(List.of())
                .build();
    }
}
