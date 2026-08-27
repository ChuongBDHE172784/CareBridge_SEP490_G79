package com.carebridge.backend.triage;

import com.carebridge.backend.triage.entity.HealthMemoryEntry;
import com.carebridge.backend.triage.policy.HealthMemorySummaryPolicy;
import com.carebridge.backend.triage.repository.HealthMemoryEntryRepository;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.HealthMemoryProperties;
import com.carebridge.backend.triage.service.impl.HealthMemoryServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.COMPLETED_AT;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.SESSION_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.USER_A;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.BABY_1;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeCompletedSession;
import static com.carebridge.backend.triage.HealthMemoryContextTestFactory.makeProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CB-TRIAGE-THMC-IMP-001-TEST — write path (THMC-TC-01/02/03/05).
 * Real HealthMemorySummaryPolicy + real properties over mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class HealthMemoryWriteTest {

    @Mock private HealthMemoryEntryRepository memoryRepository;
    @Mock private IIntakeSessionRepository sessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HealthMemoryServiceImpl service(HealthMemoryProperties properties) {
        return new HealthMemoryServiceImpl(memoryRepository, sessionRepository,
                new HealthMemorySummaryPolicy(objectMapper), properties);
    }

    @Test
    void thmcTc01_completedSession_writesExactlyOneMemoryWithLinkagePayloadAndStage() throws Exception {
        // Oracle: Roadmap III.1(a) / BR-THMC-001 / baseline :999-1012 / TDS §5.2 payload schema
        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A))
                .thenReturn(Optional.of(makeCompletedSession()));
        when(memoryRepository.existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1)).thenReturn(false);
        when(memoryRepository.save(any(HealthMemoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<HealthMemoryEntry> result =
                service(makeProperties()).writeFromCompletedSession(SESSION_1, USER_A);

        ArgumentCaptor<HealthMemoryEntry> captor = ArgumentCaptor.forClass(HealthMemoryEntry.class);
        verify(memoryRepository, times(1)).save(captor.capture());
        HealthMemoryEntry saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);                       // BR-THMC-001
        assertThat(saved.getSourceSessionId()).isEqualTo(SESSION_1);           // BR-THMC-001
        assertThat(saved.getBabyProfileId()).isEqualTo(BABY_1);                // session subject
        assertThat(saved.getMotherProfileId()).isNull();
        assertThat(saved.getRelatedStage()).isEqualTo(TriageStage.INFANT);     // Roadmap III.1a related_stage
        assertThat(saved.getSummaryText()).isNotBlank().contains("YELLOW");    // TDS §8.1 buildSummary
        JsonNode payload = objectMapper.readTree(saved.getMemoryPayloadJson());// Logic Issue L1
        assertThat(payload.path("schemaVersion").asText()).isEqualTo("1.0");   // TDS §5.2
        assertThat(payload.path("riskLevel").asText()).isEqualTo("YELLOW");
        assertThat(payload.path("normalizedSymptoms")).extracting(JsonNode::asText)
                .containsExactly("fever", "cough");
        assertThat(payload.path("sourceSessionId").asText()).isEqualTo(SESSION_1.toString());
        // BR-THMC-005 / ADR-THMC-002: computed from completedAt (2h in the past), not now()
        assertThat(saved.getExpiresAt()).isEqualTo(COMPLETED_AT.plus(30, ChronoUnit.DAYS));
        assertThat(result).isPresent();
    }

    @Test
    void thmcTc02_nonCompletedSessions_writeNothing_withPositiveControl() {
        // Oracle: BR-THMC-001 status gate / TriageService.java completion gating
        HealthMemoryServiceImpl service = service(makeProperties());

        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A)).thenReturn(
                Optional.of(makeCompletedSession(s -> s.setStatus(IntakeStatus.NEED_MORE_INFO))));
        assertThat(service.writeFromCompletedSession(SESSION_1, USER_A)).isEmpty();

        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A)).thenReturn(
                Optional.of(makeCompletedSession(s -> s.setStatus(IntakeStatus.FAILED))));
        assertThat(service.writeFromCompletedSession(SESSION_1, USER_A)).isEmpty();

        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A)).thenReturn(
                Optional.of(makeCompletedSession(s -> s.setRiskLevel(null))));
        assertThat(service.writeFromCompletedSession(SESSION_1, USER_A)).isEmpty();

        verify(memoryRepository, never()).save(any());
        // Positive control — the method executed a session lookup per variant (not a no-op shell)
        verify(sessionRepository, times(3)).findByIdAndUserId(SESSION_1, USER_A);
    }

    @Test
    void thmcTc03_replayedCompletionEvent_neverWritesDuplicateActiveMemory() {
        // Oracle: BR-THMC-001 idempotency / ADR-THMC-001 Decision (exists-guard)
        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A))
                .thenReturn(Optional.of(makeCompletedSession()));
        when(memoryRepository.existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1)).thenReturn(true);

        Optional<HealthMemoryEntry> result =
                service(makeProperties()).writeFromCompletedSession(SESSION_1, USER_A);

        assertThat(result).isEmpty();
        verify(memoryRepository, never()).save(any());
        verify(memoryRepository, times(1)).existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1);
    }

    @Test
    void thmcTc05_expiresAtHonorsConfiguredTtlOverride() {
        // Oracle: BR-THMC-005 / ADR-THMC-002 — expires_at = completed_at + ttlDays (configurable)
        when(sessionRepository.findByIdAndUserId(SESSION_1, USER_A))
                .thenReturn(Optional.of(makeCompletedSession()));
        when(memoryRepository.existsBySourceSessionIdAndDeletedAtIsNull(SESSION_1)).thenReturn(false);
        when(memoryRepository.save(any(HealthMemoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service(makeProperties(p -> p.setTtlDays(7))).writeFromCompletedSession(SESSION_1, USER_A);

        ArgumentCaptor<HealthMemoryEntry> captor = ArgumentCaptor.forClass(HealthMemoryEntry.class);
        verify(memoryRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt())
                .isEqualTo(COMPLETED_AT.plus(7, ChronoUnit.DAYS));
    }
}
