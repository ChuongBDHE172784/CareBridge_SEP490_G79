package com.carebridge.backend.journey;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
import com.carebridge.backend.journey.entity.LifecycleSafetyOutcome;
import com.carebridge.backend.journey.repository.LifecycleSafetyOutcomeInsertRepository;
import com.carebridge.backend.journey.service.LifecycleSafetyOutcomeProjector;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LifecycleSafetyOutcomeProjectorTest {
    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private TriageEmergencyEscalationLinkRepository triageEscalationLinkRepository;
    @Mock private LifecycleSafetyOutcomeInsertRepository insertRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleSafetyMetrics metrics;
    @InjectMocks private LifecycleSafetyOutcomeProjector projector;

    @Test
    void ov01E2e008_greenProjectsOnceForEveryTypedOriginWithoutEmergencySideEffects() {
        for (OriginDashboard dashboard : OriginDashboard.values()) {
            reset(intakeSessionRepository, triageEscalationLinkRepository, insertRepository,
                    auditService, metrics);
            UUID ownerId = UUID.randomUUID();
            UUID intakeId = UUID.randomUUID();
            UUID journeyId = UUID.randomUUID();
            UUID originReferenceId = dashboard == OriginDashboard.MOTHER_JOURNEY
                    ? journeyId
                    : UUID.randomUUID();
            TriageStage stage = dashboard == OriginDashboard.MOTHER_JOURNEY
                    ? TriageStage.POSTPARTUM
                    : TriageStage.INFANT;
            IntakeSession intake = IntakeSession.builder()
                    .id(intakeId)
                    .userId(ownerId)
                    .journeyId(journeyId)
                    .riskLevel(RiskLevel.GREEN)
                    .status(IntakeStatus.COMPLETED)
                    .stage(stage)
                    .originDashboard(dashboard)
                    .originReferenceId(originReferenceId)
                    .completedAt(Instant.now())
                    .build();
            UUID eventId = UUID.randomUUID();
            when(intakeSessionRepository.findByIdAndUserId(intakeId, ownerId))
                    .thenReturn(Optional.of(intake));
            when(insertRepository.insertIfAbsent(any()))
                    .thenReturn(eventId)
                    .thenReturn(null);

            var first = projector.ensureProjected(intakeId, ownerId);
            var replay = projector.ensureProjected(intakeId, ownerId);

            assertThat(first.created()).isTrue();
            assertThat(first.outcomeId()).isEqualTo(eventId);
            assertThat(replay.created()).isFalse();
            assertThat(replay.outcomeId()).isNull();
            ArgumentCaptor<LifecycleSafetyOutcome> events =
                    ArgumentCaptor.forClass(LifecycleSafetyOutcome.class);
            verify(insertRepository, times(2)).insertIfAbsent(events.capture());
            assertThat(events.getAllValues()).allSatisfy(event -> {
                assertThat(event.getOwnerUserId()).isEqualTo(ownerId);
                assertThat(event.getJourneyId()).isEqualTo(journeyId);
                assertThat(event.getIntakeSessionId()).isEqualTo(intakeId);
                assertThat(event.getRiskLevel()).isEqualTo(RiskLevel.GREEN);
                assertThat(event.getOriginDashboard()).isEqualTo(dashboard);
                assertThat(event.getOriginReferenceId()).isEqualTo(originReferenceId);
                assertThat(event.getOriginAction()).isEqualTo(OriginAction.forDashboard(dashboard));
                assertThat(event.getEmergencySessionId()).isNull();
            });
            verifyNoInteractions(triageEscalationLinkRepository);
            verify(auditService).log(eq(AuditAction.AI_TRIAGE), eq(ownerId),
                    eq("MotherJourneyEvent"), eq(eventId.toString()), anyMap());
            verify(metrics).record(LifecycleSafetyMetrics.Boundary.PROJECTION,
                    LifecycleSafetyMetrics.Outcome.CREATED);
            verify(metrics).record(LifecycleSafetyMetrics.Boundary.PROJECTION,
                    LifecycleSafetyMetrics.Outcome.REPLAYED);
        }
    }

    @Test
    void redOutcomeUsesCanonicalEmergencySourceAndProjectsOnce() {
        UUID ownerId = UUID.randomUUID();
        UUID intakeId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID emergencyId = UUID.randomUUID();
        IntakeSession intake = IntakeSession.builder()
                .id(intakeId)
                .userId(ownerId)
                .journeyId(journeyId)
                .riskLevel(RiskLevel.RED)
                .status(IntakeStatus.COMPLETED)
                .stage(TriageStage.POSTPARTUM)
                .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                .originReferenceId(journeyId)
                .completedAt(Instant.now())
                .build();
        UUID eventId = UUID.randomUUID();
        when(intakeSessionRepository.findByIdAndUserId(intakeId, ownerId))
                .thenReturn(Optional.of(intake));
        when(triageEscalationLinkRepository.findEmergencySessionId(intakeId, ownerId))
                .thenReturn(Optional.of(emergencyId));
        when(insertRepository.insertIfAbsent(any())).thenReturn(eventId);

        var result = projector.ensureProjected(intakeId, ownerId);

        assertThat(result.created()).isTrue();
        assertThat(result.outcomeId()).isEqualTo(eventId);
        ArgumentCaptor<LifecycleSafetyOutcome> event =
                ArgumentCaptor.forClass(LifecycleSafetyOutcome.class);
        verify(insertRepository).insertIfAbsent(event.capture());
        assertThat(event.getValue().getEmergencySessionId()).isEqualTo(emergencyId);
        assertThat(event.getValue().getIntakeSessionId()).isEqualTo(intakeId);
        assertThat(event.getValue().getJourneyId()).isEqualTo(journeyId);
    }
}
