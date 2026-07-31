package com.carebridge.backend.triage.service;

import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyTriageStageClassifier;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.LifecycleConsentValidator;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.exception.TriageException;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LifecycleIntakeBindingService {
    private final LifecycleConsentValidator lifecycleConsentValidator;
    private final MotherJourneyRepository motherJourneyRepository;
    private final BabyProfileRepository babyProfileRepository;
    private final BabyTriageStageClassifier babyTriageStageClassifier;
    private final LifecycleSafetyMetrics metrics;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public LifecycleIntakeBindingService(
            LifecycleConsentValidator lifecycleConsentValidator,
            MotherJourneyRepository motherJourneyRepository,
            BabyProfileRepository babyProfileRepository,
            BabyTriageStageClassifier babyTriageStageClassifier,
            LifecycleSafetyMetrics metrics,
            @Value("${triage.continuation.ttl:PT168H}") Duration ttl) {
        this(lifecycleConsentValidator, motherJourneyRepository, babyProfileRepository,
                babyTriageStageClassifier, metrics, ttl, Clock.systemUTC());
    }

    public LifecycleIntakeBindingService(
            LifecycleConsentValidator lifecycleConsentValidator,
            MotherJourneyRepository motherJourneyRepository,
            BabyProfileRepository babyProfileRepository,
            Duration ttl) {
        this(lifecycleConsentValidator, motherJourneyRepository, babyProfileRepository,
                new BabyTriageStageClassifier(), new LifecycleSafetyMetrics(), ttl, Clock.systemUTC());
    }

    public LifecycleIntakeBindingService(
            LifecycleConsentValidator lifecycleConsentValidator,
            MotherJourneyRepository motherJourneyRepository,
            BabyProfileRepository babyProfileRepository,
            BabyTriageStageClassifier babyTriageStageClassifier,
            LifecycleSafetyMetrics metrics,
            Duration ttl,
            Clock clock) {
        this.lifecycleConsentValidator = lifecycleConsentValidator;
        this.motherJourneyRepository = motherJourneyRepository;
        this.babyProfileRepository = babyProfileRepository;
        this.babyTriageStageClassifier = babyTriageStageClassifier;
        this.metrics = metrics;
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Continuation TTL must be positive");
        }
        this.ttl = ttl;
        this.clock = clock;
    }

    public LifecycleBinding bindForStart(
            StartIntakeConversationRequest request, TriageStage stage, UUID ownerUserId) {
        boolean any = request.getJourneyId() != null || request.getOriginDashboard() != null
                || request.getOriginReferenceId() != null;
        if (!any) {
            return null;
        }
        try {
            if (request.getClientRequestId() == null || request.getClientRequestId().isBlank()) {
                throw error(HttpStatus.BAD_REQUEST, "TRIAGE-012",
                        "Lifecycle intake requires clientRequestId");
            }
            if (request.getOriginDashboard() == null || request.getOriginReferenceId() == null
                    || (request.getOriginDashboard() == OriginDashboard.MOTHER_JOURNEY
                        && request.getJourneyId() == null)
                    || (request.getOriginDashboard() == OriginDashboard.BABY_PROFILE
                        && (request.getJourneyId() != null || request.getBabyProfileId() == null))) {
                throw error(HttpStatus.BAD_REQUEST, "TRIAGE-012", "Incomplete lifecycle origin");
            }
            if (request.getOriginDashboard() == OriginDashboard.MOTHER_JOURNEY) {
                lifecycleConsentValidator.ensureEligibleForMutation(ownerUserId);
            }
            validateOrigin(ownerUserId, request.getJourneyId(), stage, request.getOriginDashboard(),
                    request.getOriginReferenceId(), request.getBabyProfileId());
            LifecycleBinding binding = new LifecycleBinding(
                    request.getJourneyId(), request.getOriginDashboard(),
                    request.getOriginReferenceId(), stage, UUID.randomUUID(),
                    Instant.now(clock).plus(ttl));
            return binding;
        } catch (com.carebridge.backend.common.exception.BusinessException | TriageException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.REJECTED);
            throw exception;
        } catch (RuntimeException exception) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.FAILED);
            throw exception;
        }
    }

    /** Records creation only after the new lifecycle-bound intake has been persisted. */
    public void recordCreated() {
        metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                LifecycleSafetyMetrics.Outcome.CREATED);
    }

    public void validateReplay(IntakeSession session, LifecycleBinding requested) {
        boolean same = requested != null
                && Objects.equals(session.getJourneyId(), requested.journeyId())
                && session.getOriginDashboard() == requested.originDashboard()
                && Objects.equals(session.getOriginReferenceId(), requested.originReferenceId())
                && session.getStage() == requested.stage();
        if (!same) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.REJECTED);
            throw error(HttpStatus.CONFLICT, "TRIAGE-016", "Intake context conflict");
        }
        metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                LifecycleSafetyMetrics.Outcome.REPLAYED);
    }

    public void renewForTerminal(IntakeSession session) {
        if (session.getContinuationToken() == null || session.getContinuationExpiresAt() == null) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.FAILED);
            throw new IllegalStateException("Lifecycle continuation state is incomplete");
        }
        if (session.getContinuationAcknowledgedAt() != null) {
            return;
        }
        Instant now = Instant.now(clock);
        boolean expired = !session.getContinuationExpiresAt().isAfter(now);
        session.setContinuationExpiresAt(now.plus(ttl));
        if (expired) {
            metrics.record(LifecycleSafetyMetrics.Boundary.CONTINUATION,
                    LifecycleSafetyMetrics.Outcome.RECOVERED);
        }
    }

    public void revalidate(IntakeSession session) {
        if (session.getOriginDashboard() == OriginDashboard.MOTHER_JOURNEY) {
            lifecycleConsentValidator.ensureEligibleForRead(session.getUserId());
        }
        validateOrigin(session.getUserId(), session.getJourneyId(), session.getStage(),
                session.getOriginDashboard(), session.getOriginReferenceId(),
                session.getBabyProfileId());
    }

    private void validateOrigin(
            UUID ownerUserId, UUID journeyId, TriageStage stage,
            OriginDashboard dashboard, UUID originReferenceId, UUID babyProfileId) {
        if (dashboard == OriginDashboard.MOTHER_JOURNEY) {
            var journey = motherJourneyRepository.findById(journeyId)
                    .filter(candidate -> candidate.getOwnerUserId().equals(ownerUserId))
                    .filter(candidate -> candidate.getStatus() == JourneyStatus.ACTIVE)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "JOURNEY-002", "Journey not found"));
            JourneyType expected = switch (stage) {
                case PRECONCEPTION -> JourneyType.PRE_PREGNANCY;
                case PREGNANCY -> JourneyType.PREGNANCY;
                case POSTPARTUM -> JourneyType.POSTPARTUM;
                default -> null;
            };
            if (expected == null || journey.getJourneyType() != expected
                    || !originReferenceId.equals(journeyId)) {
                throw error(HttpStatus.CONFLICT, "TRIAGE-015", "Continuation origin unavailable");
            }
            return;
        }
        if (journeyId != null || !stage.isPediatric() || babyProfileId == null
                || !originReferenceId.equals(babyProfileId)) {
            throw error(HttpStatus.CONFLICT, "TRIAGE-015", "Continuation origin unavailable");
        }
        var baby = babyProfileRepository.findByIdAndOwnerUserId(babyProfileId, ownerUserId)
                .filter(candidate -> candidate.getStatus() == BabyProfileStatus.ACTIVE)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "TRIAGE-015", "Continuation origin unavailable"));
        if (babyTriageStageClassifier.classify(baby.getBirthDate()) != stage) {
            throw error(HttpStatus.CONFLICT, "TRIAGE-015", "Continuation origin unavailable");
        }
    }

    private TriageException error(HttpStatus status, String code, String message) {
        return new TriageException(status, code, message);
    }
}
