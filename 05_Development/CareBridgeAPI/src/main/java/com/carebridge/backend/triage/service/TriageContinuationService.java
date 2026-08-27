package com.carebridge.backend.triage.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.dto.response.ContinuationDescriptor;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TriageContinuationService implements ITriageContinuationService {
    private final IIntakeSessionRepository intakeSessionRepository;
    private final LifecycleIntakeBindingService bindingService;
    private final LifecycleSafetyMetrics metrics;
    private final Clock clock;

    @Autowired
    public TriageContinuationService(
            IIntakeSessionRepository intakeSessionRepository,
            LifecycleIntakeBindingService bindingService,
            LifecycleSafetyMetrics metrics) {
        this(intakeSessionRepository, bindingService, metrics, Clock.systemUTC());
    }

    public TriageContinuationService(
            IIntakeSessionRepository intakeSessionRepository,
            LifecycleIntakeBindingService bindingService) {
        this(intakeSessionRepository, bindingService,
                new LifecycleSafetyMetrics(), Clock.systemUTC());
    }

    public TriageContinuationService(
            IIntakeSessionRepository intakeSessionRepository,
            LifecycleIntakeBindingService bindingService,
            LifecycleSafetyMetrics metrics,
            Clock clock) {
        this.intakeSessionRepository = intakeSessionRepository;
        this.bindingService = bindingService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ContinuationDescriptor resolve(UUID ownerUserId, String token) {
        try {
            IntakeSession session = requireActive(ownerUserId, parseToken(token));
            revalidateOrigin(session);
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.RECOVERED);
            return descriptor(session);
        } catch (TriageException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.REJECTED);
            throw exception;
        } catch (RuntimeException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.FAILED);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void acknowledge(UUID ownerUserId, String token) {
        try {
            UUID parsedToken = parseToken(token);
            IntakeSession session = intakeSessionRepository
                    .findForUpdateByUserIdAndContinuationToken(ownerUserId, parsedToken)
                    .orElseThrow(this::unavailable);
            requireTerminalOutcome(session);
            if (session.getContinuationAcknowledgedAt() != null) {
                metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                        LifecycleSafetyMetrics.Outcome.REPLAYED);
                return;
            }
            Instant now = Instant.now(clock);
            if (session.getContinuationExpiresAt() == null
                    || !session.getContinuationExpiresAt().isAfter(now)) {
                throw unavailable();
            }
            revalidateOrigin(session);
            session.setContinuationAcknowledgedAt(now);
            intakeSessionRepository.save(session);
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.ACKNOWLEDGED);
        } catch (TriageException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.REJECTED);
            throw exception;
        } catch (RuntimeException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.FAILED);
            throw exception;
        }
    }

    private void revalidateOrigin(IntakeSession session) {
        try {
            bindingService.revalidate(session);
        } catch (BusinessException | TriageException exception) {
            throw new TriageException(HttpStatus.CONFLICT, "TRIAGE-015",
                    "Continuation origin unavailable");
        }
    }

    private UUID parseToken(String token) {
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw unavailable();
        }
    }

    private IntakeSession requireActive(UUID ownerUserId, UUID token) {
        IntakeSession session = intakeSessionRepository
                .findByUserIdAndContinuationToken(ownerUserId, token)
                .orElseThrow(this::unavailable);
        requireTerminalOutcome(session);
        if (session.getContinuationAcknowledgedAt() != null
                || session.getContinuationExpiresAt() == null
                || !session.getContinuationExpiresAt().isAfter(Instant.now(clock))) {
            throw unavailable();
        }
        return session;
    }

    private void requireTerminalOutcome(IntakeSession session) {
        if (session.getStatus() != IntakeStatus.COMPLETED
                || session.getRiskLevel() == null) {
            throw unavailable();
        }
    }

    private ContinuationDescriptor descriptor(IntakeSession session) {
        return ContinuationDescriptor.builder()
                .intakeSessionId(session.getId())
                .status(session.getStatus().name())
                .riskLevel(session.getRiskLevel() == null ? null : session.getRiskLevel().name())
                .stage(session.getStage().name())
                .journeyId(session.getJourneyId())
                .originDashboard(session.getOriginDashboard())
                .originReferenceId(session.getOriginReferenceId())
                .originAction(OriginAction.forDashboard(session.getOriginDashboard()))
                .build();
    }

    private TriageException unavailable() {
        return new TriageException(HttpStatus.NOT_FOUND, "TRIAGE-014", "Continuation unavailable");
    }
}
