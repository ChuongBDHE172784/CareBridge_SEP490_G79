package com.carebridge.backend.journey.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
import com.carebridge.backend.journey.entity.LifecycleSafetyOutcome;
import com.carebridge.backend.journey.repository.LifecycleSafetyOutcomeInsertRepository;
import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import com.carebridge.backend.triage.service.LifecycleSafetyMetrics;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LifecycleSafetyOutcomeProjector implements ILifecycleSafetyOutcomeProjector {
    private final IIntakeSessionRepository intakeSessionRepository;
    private final TriageEmergencyEscalationLinkRepository triageEscalationLinkRepository;
    private final LifecycleSafetyOutcomeInsertRepository insertRepository;
    private final AuditService auditService;
    private final LifecycleSafetyMetrics metrics;

    @Override
    @Transactional
    public ProjectionResult ensureProjected(UUID intakeSessionId, UUID ownerUserId) {
        try {
            var intake = intakeSessionRepository.findByIdAndUserId(intakeSessionId, ownerUserId)
                    .orElseThrow(() -> new IllegalStateException("Completed intake is unavailable"));
            if (intake.getOriginDashboard() == null) {
                metrics.record(LifecycleSafetyMetrics.Boundary.PROJECTION,
                        LifecycleSafetyMetrics.Outcome.REJECTED);
                return ProjectionResult.skipped();
            }
            UUID emergencyId = null;
            if (intake.getRiskLevel() == RiskLevel.RED) {
                emergencyId = triageEscalationLinkRepository
                        .findEmergencySessionId(intakeSessionId, ownerUserId)
                        .orElse(null);
            }
            LifecycleSafetyOutcome outcome = LifecycleSafetyOutcome.builder()
                    .id(deterministicOutcomeId(intakeSessionId))
                    .ownerUserId(ownerUserId)
                    .journeyId(intake.getJourneyId())
                    .intakeSessionId(intakeSessionId)
                    .emergencySessionId(emergencyId)
                    .riskLevel(intake.getRiskLevel())
                    .stage(intake.getStage())
                    .originDashboard(intake.getOriginDashboard())
                    .originReferenceId(intake.getOriginReferenceId())
                    .originAction(OriginAction.forDashboard(intake.getOriginDashboard()))
                    .occurredAt(intake.getCompletedAt())
                    .recordedAt(Instant.now())
                    .build();
            UUID createdId = insertRepository.insertIfAbsent(outcome);
            if (createdId != null) {
                auditService.log(AuditAction.AI_TRIAGE, ownerUserId, "LifecycleSafetyOutcome",
                        createdId.toString(), Map.of(
                                "event", "SAFETY_OUTCOME_PROJECTED",
                                "riskLevel", intake.getRiskLevel().name(),
                                "stage", intake.getStage().name(),
                                "originDashboard", intake.getOriginDashboard().name(),
                                "originReferenceId", intake.getOriginReferenceId().toString()));
                metrics.record(LifecycleSafetyMetrics.Boundary.PROJECTION,
                        LifecycleSafetyMetrics.Outcome.CREATED);
                return new ProjectionResult(true, createdId);
            }
            metrics.record(LifecycleSafetyMetrics.Boundary.PROJECTION,
                    LifecycleSafetyMetrics.Outcome.REPLAYED);
            return ProjectionResult.skipped();
        } catch (RuntimeException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.PROJECTION,
                    LifecycleSafetyMetrics.Outcome.FAILED);
            throw exception;
        }
    }

    /**
     * Mirrors the retired mother_journey_events (legacy_source='SAFETY_OUTCOME', legacy_id)
     * uniqueness key: exactly one canonical SAFETY_OUTCOME audit event per completed intake,
     * enforced through ON CONFLICT (audit_event_id) DO NOTHING.
     */
    private UUID deterministicOutcomeId(UUID intakeSessionId) {
        return UUID.nameUUIDFromBytes(
                ("SAFETY_OUTCOME:" + intakeSessionId).getBytes(StandardCharsets.UTF_8));
    }
}
