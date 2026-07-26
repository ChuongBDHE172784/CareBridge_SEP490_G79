package com.carebridge.backend.journey;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
import com.carebridge.backend.journey.entity.LifecycleSafetyOutcome;
import com.carebridge.backend.journey.repository.LifecycleSafetyOutcomeInsertRepository;
import com.carebridge.backend.journey.service.LifecycleSafetyOutcomeProjector;
import com.carebridge.backend.triage.IntakeStatus;
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
