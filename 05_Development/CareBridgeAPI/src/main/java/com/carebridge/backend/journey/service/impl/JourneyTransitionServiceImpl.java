package com.carebridge.backend.journey.service.impl;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.dto.PregnancyOutcomeResponse;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyTransitionType;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.MotherJourneyTransition;
import com.carebridge.backend.journey.entity.PregnancyOutcomeEvidence;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.event.MotherJourneyCreated;
import com.carebridge.backend.journey.event.MotherJourneyTransitioned;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.MotherJourneyTransitionRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import com.carebridge.backend.journey.service.GestationalDatingResolution;
import com.carebridge.backend.journey.service.GestationalDatingResolver;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class JourneyTransitionServiceImpl implements IJourneyTransitionService {

    private static final ZoneId CAREBRIDGE_BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final MotherJourneyRepository journeyRepository;
    private final MotherJourneyTransitionRepository transitionRepository;
    private final PregnancyOutcomeEvidenceRepository outcomeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final JourneyTransitionPolicy transitionPolicy;
    private final ApplicationEventPublisher eventPublisher;
    private final IJourneyOnboardingService onboardingService;
    private final Clock clock;
    private final GestationalDatingResolver datingResolver;

    @Autowired
    public JourneyTransitionServiceImpl(
            MotherJourneyRepository journeyRepository,
            MotherJourneyTransitionRepository transitionRepository,
            PregnancyOutcomeEvidenceRepository outcomeRepository,
            UserRepository userRepository,
            AuditService auditService,
            JourneyTransitionPolicy transitionPolicy,
            ApplicationEventPublisher eventPublisher,
            IJourneyOnboardingService onboardingService) {
        this(journeyRepository, transitionRepository, outcomeRepository, userRepository, auditService,
                transitionPolicy, eventPublisher, onboardingService, Clock.systemUTC(),
                new GestationalDatingResolver());
    }

    public JourneyTransitionServiceImpl(
            MotherJourneyRepository journeyRepository,
            MotherJourneyTransitionRepository transitionRepository,
            UserRepository userRepository,
            AuditService auditService,
            JourneyTransitionPolicy transitionPolicy,
            ApplicationEventPublisher eventPublisher,
            IJourneyOnboardingService onboardingService,
            Clock clock) {
        this(journeyRepository, transitionRepository, null, userRepository, auditService,
                transitionPolicy, eventPublisher, onboardingService, clock,
                new GestationalDatingResolver());
    }

    public JourneyTransitionServiceImpl(
            MotherJourneyRepository journeyRepository,
            MotherJourneyTransitionRepository transitionRepository,
            PregnancyOutcomeEvidenceRepository outcomeRepository,
            UserRepository userRepository,
            AuditService auditService,
            JourneyTransitionPolicy transitionPolicy,
            ApplicationEventPublisher eventPublisher,
            IJourneyOnboardingService onboardingService,
            Clock clock) {
        this(journeyRepository, transitionRepository, outcomeRepository, userRepository, auditService,
                transitionPolicy, eventPublisher, onboardingService, clock,
                new GestationalDatingResolver());
    }

    public JourneyTransitionServiceImpl(
            MotherJourneyRepository journeyRepository,
            MotherJourneyTransitionRepository transitionRepository,
            PregnancyOutcomeEvidenceRepository outcomeRepository,
            UserRepository userRepository,
            AuditService auditService,
            JourneyTransitionPolicy transitionPolicy,
            ApplicationEventPublisher eventPublisher,
            IJourneyOnboardingService onboardingService,
            Clock clock,
            GestationalDatingResolver datingResolver) {
        this.journeyRepository = journeyRepository;
        this.transitionRepository = transitionRepository;
        this.outcomeRepository = outcomeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.transitionPolicy = transitionPolicy;
        this.eventPublisher = eventPublisher;
        this.onboardingService = onboardingService;
        this.clock = clock;
        this.datingResolver = datingResolver;
    }

    @Override
    public PregnancyOutcomeResponse recordPregnancyOutcome(
            UUID ownerId, UUID journeyId, RecordPregnancyOutcomeRequest request) {
        onboardingService.ensureEligible(ownerId);
        if (outcomeRepository == null) {
            throw new IllegalStateException("Pregnancy outcome repository is unavailable");
        }
        MotherJourney current = journeyRepository.findByIdForUpdate(journeyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "JOURNEY-010", "Journey not found"));
        if (!current.getOwnerUserId().equals(ownerId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "JOURNEY_ACCESS_DENIED", "Access denied");
        }
        if (current.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "OUTCOME_STAGE_CONFLICT", "Journey is not active");
        }

        String semanticHash = outcomeSemanticHash(request);
        var priorSubmission = priorSubmissionInCurrentPregnancyEpoch(current, request.getSubmissionId());
        if (priorSubmission.isPresent()) {
            PregnancyOutcomeEvidence prior = priorSubmission.get();
            if (!prior.getSemanticHash().equals(semanticHash)) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "OUTCOME_SUBMISSION_CONFLICT",
                        "Submission identity was already used with different outcome data");
            }
            UUID transitionId = transitionRepository
                    .findFirstByJourneyIdAndJourneyVersionOrderByRecordedAtDesc(
                            journeyId, prior.getJourneyVersion())
                    .map(MotherJourneyTransition::getId)
                    .orElse(null);
            return toOutcomeResponse(prior, current, transitionId);
        }

        if (current.getVersion() != request.getExpectedJourneyVersion()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "JOURNEY_VERSION_CONFLICT",
                    "Journey version is stale");
        }
        validateOutcomeRequest(current, request);

        var previous = previousOutcomeInCurrentPregnancyEpoch(current);
        boolean requiresCorrection = current.getJourneyType() == JourneyType.POSTPARTUM
                || previous.map(value -> value.getOutcomeType().transitionsToPostpartum())
                        .orElse(false);
        if (requiresCorrection && !request.isCorrection()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "OUTCOME_CORRECTION_REQUIRED",
                    "Changing an existing outcome requires correction=true");
        }

        JourneyType fromStage = current.getJourneyType();
        LocalDate oldDeliveryDate = current.getDeliveryDate();
        GestationalDatingBasis oldDatingBasis = current.getGestationalDatingBasis();
        Long oldDatingRevision = current.getGestationalDatingRevision();
        Instant serverRecordedAt = Instant.now(clock);
        JourneyType targetStage = transitionPolicy.outcomeTargetStage(
                current.getJourneyType(), request.getOutcomeType(), request.isCorrection());
        boolean transitionsToPostpartum = targetStage == JourneyType.POSTPARTUM
                && current.getJourneyType() == JourneyType.PREGNANCY;
        current.setPregnancyOutcome(request.getOutcomeType());
        current.setPregnancyOutcomeDate(request.getOutcomeDate());
        if (transitionsToPostpartum) {
            current.setJourneyType(targetStage);
        }
        if (request.getOutcomeType() == PregnancyOutcomeType.LIVE_BIRTH) {
            current.setDeliveryDate(request.getOutcomeDate());
        } else {
            current.setDeliveryDate(null);
        }
        if (transitionsToPostpartum) {
            // The current row is no longer pregnancy-eligible after an
            // authoritative outcome. The immutable transition spine retains
            // the prior revision for repair/history reconstruction.
            current.setGestationalDatingBasis(null);
            current.setGestationalDatingRevision(null);
            current.setGestationalDatingEffectiveAt(null);
        }

        MotherJourney saved;
        try {
            saved = journeyRepository.saveAndFlush(current);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw optimisticConflict();
        }

        int revisionNumber = previous.map(PregnancyOutcomeEvidence::getRevisionNumber)
                .orElse(0) + 1;
        PregnancyOutcomeEvidence evidence = PregnancyOutcomeEvidence.builder()
                .journeyId(journeyId)
                .ownerUserId(ownerId)
                .submissionId(request.getSubmissionId())
                .outcomeType(request.getOutcomeType())
                .outcomeDate(request.getOutcomeDate())
                .source(request.getSource())
                .actorUserId(ownerId)
                .reason(request.getReason().trim())
                .effectiveAt(request.getEffectiveAt())
                .revisionNumber(revisionNumber)
                .supersedesEvidenceId(previous.map(PregnancyOutcomeEvidence::getId).orElse(null))
                .journeyVersion(saved.getVersion())
                .semanticHash(semanticHash)
                .correction(request.isCorrection())
                .build();
        try {
            evidence = outcomeRepository.saveAndFlush(evidence);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "OUTCOME_SUBMISSION_CONFLICT",
                    "Outcome submission conflicted with another request");
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        addChange(
                changes,
                "pregnancyOutcome",
                previous.map(PregnancyOutcomeEvidence::getOutcomeType).orElse(null),
                request.getOutcomeType());
        addChange(
                changes,
                "outcomeDate",
                previous.map(PregnancyOutcomeEvidence::getOutcomeDate).orElse(null),
                request.getOutcomeDate());
        addIfChanged(changes, "journeyType", fromStage, saved.getJourneyType());
        addIfChanged(changes, "deliveryDate", oldDeliveryDate, saved.getDeliveryDate());
        addIfChanged(changes, "gestationalDatingBasis", oldDatingBasis, saved.getGestationalDatingBasis());
        addIfChanged(changes, "gestationalDatingRevision", oldDatingRevision, saved.getGestationalDatingRevision());

        JourneyTransitionType eventType = previous.isPresent()
                ? JourneyTransitionType.OUTCOME_CORRECTED
                : JourneyTransitionType.OUTCOME_RECORDED;
        UUID correlationId = UUID.randomUUID();
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(journeyId)
                .ownerUserId(ownerId)
                .eventType(eventType)
                .fromStage(fromStage)
                .toStage(saved.getJourneyType())
                .changes(changes)
                .source(request.getSource())
                .reason(request.getReason().trim())
                .actorUserId(ownerId)
                .effectiveAt(request.getEffectiveAt())
                .recordedAt(serverRecordedAt)
                .correlationId(correlationId)
                .journeyVersion(saved.getVersion())
                .build();
        transition = transitionRepository.saveAndFlush(transition);

        auditService.log(
                AuditAction.PREGNANCY_OUTCOME_RECORDED,
                ownerId,
                "MotherJourney",
                journeyId.toString(),
                Map.of(
                        "outcomeType", request.getOutcomeType().name(),
                        "revisionNumber", revisionNumber,
                        "journeyVersion", saved.getVersion()));
        publishAfterCommit(new MotherJourneyTransitioned(
                UUID.randomUUID(),
                journeyId,
                ownerId,
                eventType,
                saved.getJourneyType(),
                saved.getStatus(),
                saved.getVersion(),
                serverRecordedAt,
                transition.getCorrelationId()));
        return toOutcomeResponse(evidence, saved, transition.getId());
    }

    @Override
    public CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId) {
        var user = userRepository.findById(callerId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "JOURNEY-001", "User not found: " + callerId));
        if (user.getRole() == null) {
            user.setRole(Role.MOTHER);
            userRepository.save(user);
        } else if (user.getRole() != Role.MOTHER) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "JOURNEY-003", "Mother role required");
        }

        onboardingService.ensureEligible(callerId);

        int contractVersion = contractVersion(request.getChecklistContractVersion());
        LocalDate serverToday = LocalDate.now(clock.withZone(CAREBRIDGE_BUSINESS_ZONE));
        GestationalDatingResolution dating = datingResolver.resolveCreate(
                request, contractVersion, serverToday);
        // Resolve stage applicability before postpartum provenance validation so
        // date-bearing non-pregnancy requests fail with the dating contract code.
        validateDirectPostpartumCreate(request);
        transitionPolicy.validateCreate(request);
        Instant requestEffectiveAt = effectiveAtOrNow(request.getEffectiveAt());
        Instant recordedAt = Instant.now(clock);
        Instant datingEffectiveAt = dating.resolved() ? recordedAt : null;
        if (journeyRepository.existsByOwnerUserIdAndStatusAndJourneyTypeIn(
                callerId, JourneyStatus.ACTIVE, JourneyTransitionPolicy.CANONICAL_STAGES)) {
            throw canonicalConflict();
        }

        Long datingRevision = dating.resolved()
                ? nextDatingRevision(null)
                : null;

        UUID careSubjectId = ensureMotherCareSubject(callerId);
        MotherJourney current = MotherJourney.builder()
                .ownerUserId(callerId)
                .careSubjectId(careSubjectId)
                .journeyType(request.getJourneyType())
                .startDate(request.getStartDate())
                .lastMenstrualDate(dating.lastMenstrualDate())
                .estimatedDueDate(dating.estimatedDueDate())
                .notes(request.getNotes())
                .status(JourneyStatus.ACTIVE)
                .dateSource(request.getDateSource())
                .dateConfidence(request.getDateConfidence())
                .gestationalDatingBasis(dating.basis())
                .gestationalDatingRevision(datingRevision)
                .gestationalDatingEffectiveAt(datingEffectiveAt)
                .build();

        MotherJourney saved;
        try {
            saved = journeyRepository.saveAndFlush(current);
            journeyRepository.linkMotherCareSubject(careSubjectId, saved.getId());
            // Canonical convention: maternal observations use care_subject_id = journey_id,
            // so the matching MOTHER care_subjects row must exist for the FK.
            journeyRepository.ensureJourneyObservationSubject(saved.getId());
        } catch (DataIntegrityViolationException exception) {
            throw canonicalConflict();
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        addChange(changes, "journeyType", null, saved.getJourneyType());
        addChange(changes, "startDate", null, saved.getStartDate());
        addChange(changes, "lastMenstrualDate", null, saved.getLastMenstrualDate());
        addChange(changes, "estimatedDueDate", null, saved.getEstimatedDueDate());
        addChange(changes, "gestationalDatingBasis", null, saved.getGestationalDatingBasis());
        addChange(changes, "canonicalLmp", null, dating.canonicalLmp());
        addChange(changes, "status", null, saved.getStatus());

        UUID correlationId = UUID.randomUUID();
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(saved.getId())
                .ownerUserId(callerId)
                .eventType(JourneyTransitionType.CREATED)
                .toStage(saved.getJourneyType())
                .changes(changes)
                .source(sourceOrUnknown(request.getDateSource()))
                .confidence(request.getDateConfidence())
                .reason(dating.resolved()
                        ? "GESTATIONAL_DATING_INITIALIZED"
                        : request.getChangeReason())
                .actorUserId(callerId)
                .effectiveAt(dating.resolved() ? datingEffectiveAt : requestEffectiveAt)
                .recordedAt(recordedAt)
                .correlationId(correlationId)
                .gestationalDatingBasis(dating.basis())
                .gestationalDatingRevision(datingRevision)
                .canonicalLmp(dating.canonicalLmp())
                .inferredSource(false)
                .journeyVersion(saved.getVersion())
                .build();
        transitionRepository.saveAndFlush(transition);

        auditService.log(
                AuditAction.JOURNEY_CREATED,
                callerId,
                "MotherJourney",
                saved.getId().toString(),
                Map.of("journeyVersion", saved.getVersion()));
        publishAfterCommit(new MotherJourneyCreated(
                UUID.randomUUID(),
                saved.getId(),
                callerId,
                saved.getJourneyType(),
                saved.getStatus(),
                saved.getVersion(),
                recordedAt,
                correlationId));
        return toCreateResponse(saved);
    }

    private UUID ensureMotherCareSubject(UUID ownerUserId) {
        UUID existing = journeyRepository.findMotherCareSubjectId(ownerUserId);
        if (existing != null) {
            return existing;
        }
        UUID candidate = UUID.randomUUID();
        journeyRepository.ensureMotherCareSubject(candidate, ownerUserId);
        existing = journeyRepository.findMotherCareSubjectId(ownerUserId);
        return existing == null ? candidate : existing;
    }

    private void validateDirectPostpartumCreate(CreateJourneyRequest request) {
        if (request.getJourneyType() != JourneyType.POSTPARTUM) {
            return;
        }
        if (request.getStartDate() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "POSTPARTUM_START_DATE_REQUIRED",
                    "Recovery start date is required");
        }
        if (request.getStartDate().isAfter(
                LocalDate.now(clock.withZone(CAREBRIDGE_BUSINESS_ZONE)))) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "POSTPARTUM_START_DATE_FUTURE",
                    "Recovery start date cannot be in the future");
        }
        boolean validConfidence = request.getDateConfidence() == JourneyDateConfidence.CONFIRMED
                || request.getDateConfidence() == JourneyDateConfidence.ESTIMATED;
        if (request.getDateSource() != JourneyDateSource.SELF_REPORTED || !validConfidence) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "POSTPARTUM_PROVENANCE_INVALID",
                    "Direct postpartum recovery requires self-reported exact or estimated provenance");
        }
    }

    @Override
    public JourneyResponse updateJourney(
            UUID ownerId, UUID journeyId, UpdateJourneyRequest request) {
        onboardingService.ensureEligible(ownerId);
        MotherJourney current = ownedJourney(ownerId, journeyId);
        if (current.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "JOURNEY-012", "Journey is not active");
        }
        validateRequestedStatus(request.getStatus());
        if ("ARCHIVED".equalsIgnoreCase(request.getStatus())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "JOURNEY-014",
                    "Status ARCHIVED can only be set by the system");
        }
        JourneyType fromStage = current.getJourneyType();
        JourneyStatus fromStatus = current.getStatus();
        LocalDate oldLmp = current.getLastMenstrualDate();
        LocalDate oldEdd = current.getEstimatedDueDate();
        LocalDate oldDelivery = current.getDeliveryDate();
        PregnancyOutcomeType oldPregnancyOutcome = current.getPregnancyOutcome();
        LocalDate oldPregnancyOutcomeDate = current.getPregnancyOutcomeDate();
        String oldNotes = current.getNotes();
        JourneyDateSource oldSource = current.getDateSource();
        JourneyDateConfidence oldConfidence = current.getDateConfidence();
        GestationalDatingBasis oldDatingBasis = current.getGestationalDatingBasis();
        Long oldDatingRevision = current.getGestationalDatingRevision();
        Instant oldDatingEffectiveAt = current.getGestationalDatingEffectiveAt();
        LocalDate oldCanonicalLmp = GestationalDatingResolver.canonicalLmp(
                oldDatingBasis, oldLmp, oldEdd);
        boolean enteringPregnancyEpoch = fromStage != JourneyType.PREGNANCY
                && request.getJourneyType() == JourneyType.PREGNANCY;
        boolean completingPregnancy = fromStage == JourneyType.PREGNANCY
                && "COMPLETED".equalsIgnoreCase(request.getStatus());
        if (enteringPregnancyEpoch && "COMPLETED".equalsIgnoreCase(request.getStatus())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "JOURNEY_NEW_EPOCH_MUST_REMAIN_ACTIVE",
                    "A new pregnancy epoch must remain active");
        }

        int contractVersion = contractVersion(request.getChecklistContractVersion());
        LocalDate serverToday = LocalDate.now(clock.withZone(CAREBRIDGE_BUSINESS_ZONE));
        GestationalDatingResolution dating = datingResolver.resolveUpdate(
                current,
                request,
                contractVersion,
                serverToday,
                enteringPregnancyEpoch
                        || (fromStage != JourneyType.PREGNANCY
                        && request.getJourneyType() == JourneyType.PREGNANCY));
        transitionPolicy.validateUpdate(current.getJourneyType(), request);
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())
                && request.getDeliveryDate() == null
                && current.getDeliveryDate() == null
                && (current.getPregnancyOutcome() == null
                        || current.getPregnancyOutcome() == PregnancyOutcomeType.LIVE_BIRTH)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "JOURNEY-013",
                    "deliveryDate is required when completing a journey");
        }
        boolean datingAuthorityChanged = dating.datingScope()
                && (enteringPregnancyEpoch
                || !dating.semanticNoOp()
                || current.getGestationalDatingBasis() == null && dating.resolved());
        Long datingRevision = dating.resolved()
                ? (datingAuthorityChanged
                ? nextDatingRevision(current.getId())
                : current.getGestationalDatingRevision())
                : null;
        Instant requestEffectiveAt = effectiveAtOrNow(request.getEffectiveAt());
        Instant recordedAt = Instant.now(clock);
        Instant datingEffectiveAt = datingAuthorityChanged ? recordedAt : null;

        // An unchanged canonical anchor is an idempotent authority no-op.  It
        // must not consume a Journey version or append an audit transition.
        boolean onlyAuthorityNoOp = dating.semanticNoOp()
                && (request.getJourneyType() == null
                || request.getJourneyType() == current.getJourneyType())
                && (request.getNotes() == null
                || Objects.equals(request.getNotes(), current.getNotes()))
                && (request.getDeliveryDate() == null
                || Objects.equals(request.getDeliveryDate(), current.getDeliveryDate()))
                && request.getStatus() == null
                && (request.getDateSource() == null
                || request.getDateSource() == current.getDateSource())
                && (request.getDateConfidence() == null
                || request.getDateConfidence() == current.getDateConfidence())
                && request.getChangeReason() == null;
        if (onlyAuthorityNoOp) {
            return toJourneyResponse(current);
        }

        if (request.getJourneyType() != null) {
            current.setJourneyType(request.getJourneyType());
        }
        if (request.getNotes() != null) {
            current.setNotes(request.getNotes());
        }
        if (dating.datingScope()) {
            current.setLastMenstrualDate(dating.lastMenstrualDate());
            current.setEstimatedDueDate(dating.estimatedDueDate());
            current.setGestationalDatingBasis(dating.basis());
            current.setGestationalDatingRevision(datingRevision);
            current.setGestationalDatingEffectiveAt(datingAuthorityChanged && dating.resolved()
                    ? datingEffectiveAt
                    : (dating.resolved() ? oldDatingEffectiveAt : null));
            current.setGestationalDatingQuarantineReasonCode(null);
        }
        if (request.getDeliveryDate() != null) {
            current.setDeliveryDate(request.getDeliveryDate());
        }
        if (enteringPregnancyEpoch) {
            // Provenance belongs to the pregnancy epoch. A new epoch must not
            // inherit the prior one; resolved epochs take only the new request
            // values and unresolved epochs remain provenance-free.
            current.setDateSource(dating.resolved() ? request.getDateSource() : null);
            current.setDateConfidence(dating.resolved() ? request.getDateConfidence() : null);
        } else {
            if (request.getDateSource() != null) {
                current.setDateSource(request.getDateSource());
            }
            if (request.getDateConfidence() != null) {
                current.setDateConfidence(request.getDateConfidence());
            }
        }
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            current.setStatus(JourneyStatus.COMPLETED);
        }
        if (enteringPregnancyEpoch) {
            // The existing journey remains the canonical identity, while this stage change
            // opens a fresh pregnancy epoch. Historical outcome evidence is append-only.
            current.setPregnancyOutcome(null);
            current.setPregnancyOutcomeDate(null);
            current.setDeliveryDate(null);
        }
        if (completingPregnancy) {
            // COMPLETED is a terminal lifecycle boundary. Keep historical raw
            // source dates for audit/history, but remove the active authority
            // so no current Plan/week can be projected after exit.
            current.setGestationalDatingBasis(null);
            current.setGestationalDatingRevision(null);
            current.setGestationalDatingEffectiveAt(null);
        }

        boolean notesChanged = !Objects.equals(oldNotes, current.getNotes());
        boolean entityChanged = fromStage != current.getJourneyType()
                || fromStatus != current.getStatus()
                || !Objects.equals(oldLmp, current.getLastMenstrualDate())
                || !Objects.equals(oldEdd, current.getEstimatedDueDate())
                || !Objects.equals(oldDelivery, current.getDeliveryDate())
                || oldPregnancyOutcome != current.getPregnancyOutcome()
                || !Objects.equals(oldPregnancyOutcomeDate, current.getPregnancyOutcomeDate())
                || !Objects.equals(oldSource, current.getDateSource())
                || !Objects.equals(oldConfidence, current.getDateConfidence())
                || !Objects.equals(oldDatingBasis, current.getGestationalDatingBasis())
                || !Objects.equals(oldDatingRevision, current.getGestationalDatingRevision())
                || !Objects.equals(oldDatingEffectiveAt, current.getGestationalDatingEffectiveAt())
                || notesChanged;
        if (!entityChanged) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "JOURNEY-020",
                    "Journey update does not contain a meaningful change");
        }

        MotherJourney saved;
        try {
            saved = journeyRepository.saveAndFlush(current);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw optimisticConflict();
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        addIfChanged(changes, "journeyType", fromStage, saved.getJourneyType());
        addIfChanged(changes, "lastMenstrualDate", oldLmp, saved.getLastMenstrualDate());
        addIfChanged(changes, "estimatedDueDate", oldEdd, saved.getEstimatedDueDate());
        addIfChanged(changes, "gestationalDatingBasis", oldDatingBasis, saved.getGestationalDatingBasis());
        addIfChanged(changes, "gestationalDatingRevision", oldDatingRevision, saved.getGestationalDatingRevision());
        if (datingAuthorityChanged) {
            addIfChanged(changes, "canonicalLmp", oldCanonicalLmp, dating.canonicalLmp());
        }
        addIfChanged(changes, "deliveryDate", oldDelivery, saved.getDeliveryDate());
        addIfChanged(changes, "pregnancyOutcome", oldPregnancyOutcome, saved.getPregnancyOutcome());
        addIfChanged(
                changes,
                "pregnancyOutcomeDate",
                oldPregnancyOutcomeDate,
                saved.getPregnancyOutcomeDate());
        addIfChanged(changes, "dateSource", oldSource, saved.getDateSource());
        addIfChanged(changes, "dateConfidence", oldConfidence, saved.getDateConfidence());
        addIfChanged(changes, "status", fromStatus, saved.getStatus());

        JourneyTransitionType eventType = resolveEventType(
                fromStage,
                saved.getJourneyType(),
                fromStatus,
                saved.getStatus(),
                changes,
                notesChanged);
        boolean datingCorrection = datingAuthorityChanged
                && oldDatingBasis != null
                && dating.resolved()
                && (!Objects.equals(oldCanonicalLmp, dating.canonicalLmp())
                || oldDatingBasis != dating.basis());
        if (enteringPregnancyEpoch) {
            eventType = JourneyTransitionType.PREGNANCY_EPOCH_STARTED;
        } else if (datingCorrection) {
            eventType = JourneyTransitionType.DATING_CORRECTED;
        }
        UUID correlationId = UUID.randomUUID();
        MotherJourneyTransition transition = MotherJourneyTransition.builder()
                .journeyId(saved.getId())
                .ownerUserId(ownerId)
                .eventType(eventType)
                .fromStage(fromStage)
                .toStage(saved.getJourneyType())
                .changes(changes)
                .source(enteringPregnancyEpoch
                        ? JourneyDateSource.SYSTEM_DERIVED
                        : sourceOrUnknown(
                                request.getDateSource() != null
                                        ? request.getDateSource()
                                        : saved.getDateSource()))
                .confidence(
                        request.getDateConfidence() != null
                                ? request.getDateConfidence()
                                : saved.getDateConfidence())
                .reason(enteringPregnancyEpoch
                        ? fromStage == JourneyType.POSTPARTUM
                        ? "POSTPARTUM_NEW_PREGNANCY_EPOCH"
                        : "PREGNANCY_EPOCH_STARTED"
                        : datingCorrection
                        ? "DATING_CORRECTED"
                        : datingAuthorityChanged
                        ? "GESTATIONAL_DATING_INITIALIZED"
                        : request.getChangeReason())
                .actorUserId(ownerId)
                .effectiveAt(datingAuthorityChanged && dating.resolved()
                        ? datingEffectiveAt
                        : enteringPregnancyEpoch ? recordedAt : requestEffectiveAt)
                .recordedAt(recordedAt)
                .correlationId(correlationId)
                .gestationalDatingBasis(datingAuthorityChanged ? dating.basis() : null)
                .gestationalDatingRevision(datingAuthorityChanged ? datingRevision : null)
                .canonicalLmp(datingAuthorityChanged ? dating.canonicalLmp() : null)
                .inferredSource(datingAuthorityChanged ? false : null)
                .pregnancyEpochStarted(enteringPregnancyEpoch)
                .supersedesDatingRevision(datingCorrection ? oldDatingRevision : null)
                .journeyVersion(saved.getVersion())
                .build();
        transitionRepository.saveAndFlush(transition);

        auditService.log(
                AuditAction.JOURNEY_UPDATED,
                ownerId,
                "MotherJourney",
                saved.getId().toString(),
                Map.of(
                        "eventType", eventType.name(),
                        "journeyVersion", saved.getVersion(),
                        "changedFields", changes.keySet()));
        publishAfterCommit(new MotherJourneyTransitioned(
                UUID.randomUUID(),
                saved.getId(),
                ownerId,
                eventType,
                saved.getJourneyType(),
                saved.getStatus(),
                saved.getVersion(),
                recordedAt,
                correlationId));
        return toJourneyResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JourneyTransitionPageResponse getHistory(
            UUID ownerId, UUID journeyId, Pageable pageable) {
        ownedJourney(ownerId, journeyId);
        var history = transitionRepository
                .findByJourneyIdOrderByRecordedAtDesc(journeyId, pageable);
        var items = history.stream()
                .map(this::toTransitionResponse)
                .toList();
        return JourneyTransitionPageResponse.builder()
                .items(items)
                .page(history.getNumber())
                .size(history.getSize())
                .totalElements(history.getTotalElements())
                .totalPages(history.getTotalPages())
                .build();
    }

    private MotherJourney ownedJourney(UUID ownerId, UUID journeyId) {
        // Serialize dating/lifecycle mutations on the existing canonical row.
        // The fallback keeps the read-only Mockito/unit seam and preserves the
        // indistinguishable 404/403 behavior for an owner mismatch.
        var locked = journeyRepository.findByIdAndOwnerUserIdForUpdate(journeyId, ownerId);
        if (locked.isPresent()) {
            return locked.get();
        }
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "JOURNEY-010", "Journey not found: " + journeyId));
        if (!journey.getOwnerUserId().equals(ownerId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "JOURNEY-011", "Access denied");
        }
        return journey;
    }

    private JourneyTransitionType resolveEventType(
            JourneyType fromStage,
            JourneyType toStage,
            JourneyStatus fromStatus,
            JourneyStatus toStatus,
            Map<String, Object> changes,
            boolean notesChanged) {
        if (fromStage != toStage) {
            return JourneyTransitionType.STAGE_CHANGED;
        }
        if (fromStatus != toStatus) {
            return JourneyTransitionType.STATUS_CHANGED;
        }
        if (changes.keySet().stream().anyMatch(this::isDateOrProvenanceField)) {
            return JourneyTransitionType.DATES_CHANGED;
        }
        if (notesChanged) {
            return JourneyTransitionType.DETAILS_CHANGED;
        }
        throw new IllegalStateException("Meaningful journey update has no transition type");
    }

    private void addIfChanged(
            Map<String, Object> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            addChange(changes, field, before, after);
        }
    }

    private void addChange(
            Map<String, Object> changes, String field, Object before, Object after) {
        if (before == null && after == null) {
            return;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("previous", jsonValue(before));
        value.put("new", jsonValue(after));
        changes.put(field, value);
    }

    private Object jsonValue(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        return value;
    }

    private Instant effectiveAtOrNow(Instant effectiveAt) {
        Instant now = Instant.now(clock);
        if (effectiveAt == null) {
            return now;
        }
        if (effectiveAt.isAfter(now.plusSeconds(300))) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "JOURNEY-019",
                    "effectiveAt cannot be more than five minutes in the future");
        }
        return effectiveAt;
    }

    private int contractVersion(Integer requested) {
        int version = requested == null ? GestationalDatingResolver.V1 : requested;
        if (version != GestationalDatingResolver.V1
                && version != GestationalDatingResolver.V2) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CHECKLIST_CONTRACT_VERSION_UNSUPPORTED",
                    "Unsupported checklist contract version");
        }
        return version;
    }

    private long nextDatingRevision(UUID journeyId) {
        if (journeyId == null) {
            return 1L;
        }
        Long maximum = transitionRepository.findMaxGestationalDatingRevision(journeyId);
        return (maximum == null ? 0L : maximum) + 1L;
    }

    private void validateRequestedStatus(String status) {
        if (status == null
                || "ACTIVE".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)
                || "ARCHIVED".equalsIgnoreCase(status)) {
            return;
        }
        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "JOURNEY-021",
                "Unsupported journey status: " + status);
    }

    private boolean isDateOrProvenanceField(String field) {
        return field.equals("lastMenstrualDate")
                || field.equals("estimatedDueDate")
                || field.equals("deliveryDate")
                || field.equals("dateSource")
                || field.equals("dateConfidence")
                || field.equals("gestationalDatingBasis")
                || field.equals("gestationalDatingRevision")
                || field.equals("canonicalLmp");
    }

    private java.util.Optional<PregnancyOutcomeEvidence> previousOutcomeInCurrentPregnancyEpoch(
            MotherJourney journey) {
        if (journey.getJourneyType() != JourneyType.PREGNANCY) {
            return outcomeRepository.findFirstByJourneyIdOrderByRevisionNumberDesc(journey.getId());
        }
        return transitionRepository.findLatestPostpartumToPregnancyEpochVersion(journey.getId())
                .map(epochVersion -> outcomeRepository
                        .findFirstByJourneyIdAfterEpochVersionOrderByRevisionNumberDesc(
                                journey.getId(), epochVersion))
                .orElseGet(() -> outcomeRepository
                        .findFirstByJourneyIdOrderByRevisionNumberDesc(journey.getId()));
    }

    private java.util.Optional<PregnancyOutcomeEvidence> priorSubmissionInCurrentPregnancyEpoch(
            MotherJourney journey, UUID submissionId) {
        if (journey.getJourneyType() != JourneyType.PREGNANCY) {
            return outcomeRepository.findByJourneyIdAndSubmissionId(journey.getId(), submissionId);
        }
        return transitionRepository.findLatestPostpartumToPregnancyEpochVersion(journey.getId())
                .map(epochVersion -> outcomeRepository
                        .findByJourneyIdAndSubmissionIdAfterEpochVersion(
                                journey.getId(), submissionId, epochVersion))
                .orElseGet(() -> outcomeRepository
                        .findByJourneyIdAndSubmissionId(journey.getId(), submissionId));
    }

    private JourneyDateSource sourceOrUnknown(JourneyDateSource source) {
        return source == null ? JourneyDateSource.UNKNOWN : source;
    }

    private void validateOutcomeRequest(
            MotherJourney journey, RecordPregnancyOutcomeRequest request) {
        if (request.getOutcomeType() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "PREGNANCY_OUTCOME_INVALID",
                    "Pregnancy outcome is required");
        }
        if (request.getSource() == null
                || request.getReason() == null
                || request.getReason().isBlank()
                || request.getEffectiveAt() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "OUTCOME_PROVENANCE_REQUIRED",
                    "Outcome source, reason, and effective time are required");
        }
        effectiveAtOrNow(request.getEffectiveAt());
        if (request.getOutcomeType() == PregnancyOutcomeType.LIVE_BIRTH
                && request.getOutcomeDate() == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "OUTCOME_DATE_REQUIRED",
                    "Live birth outcome date is required");
        }
        transitionPolicy.outcomeTargetStage(
                journey.getJourneyType(), request.getOutcomeType(), request.isCorrection());
    }

    private String outcomeSemanticHash(RecordPregnancyOutcomeRequest request) {
        String canonical = String.join(
                "|",
                request.getOutcomeType() == null ? "" : request.getOutcomeType().name(),
                request.getOutcomeDate() == null ? "" : request.getOutcomeDate().toString(),
                request.getSource() == null ? "" : request.getSource().name(),
                request.getReason() == null ? "" : request.getReason().trim(),
                request.getEffectiveAt() == null ? "" : request.getEffectiveAt().toString(),
                Boolean.toString(request.isCorrection()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private PregnancyOutcomeResponse toOutcomeResponse(
            PregnancyOutcomeEvidence evidence,
            MotherJourney journey,
            UUID transitionId) {
        return PregnancyOutcomeResponse.builder()
                .evidenceId(evidence.getId())
                .journeyId(evidence.getJourneyId())
                .outcomeType(evidence.getOutcomeType())
                .outcomeDate(evidence.getOutcomeDate())
                .journeyType(journey.getJourneyType())
                .journeyVersion(evidence.getJourneyVersion())
                .transitionId(transitionId)
                .revisionNumber(evidence.getRevisionNumber())
                .effectiveAt(evidence.getEffectiveAt())
                .recordedAt(evidence.getRecordedAt())
                .build();
    }

    private BusinessException canonicalConflict() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "JOURNEY-015",
                "An active mother lifecycle already exists");
    }

    private BusinessException optimisticConflict() {
        return new BusinessException(
                HttpStatus.CONFLICT, "JOURNEY-017", "Journey was modified concurrently");
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        eventPublisher.publishEvent(event);
                    }
                });
    }

    private CreateJourneyResponse toCreateResponse(MotherJourney journey) {
        GestationalDatingResolution dating = responseDating(journey);
        return CreateJourneyResponse.builder()
                .id(journey.getId())
                .journeyType(journey.getJourneyType().name())
                .status(journey.getStatus().name())
                .startDate(journey.getStartDate())
                .lastMenstrualDate(journey.getLastMenstrualDate())
                .estimatedDueDate(journey.getEstimatedDueDate())
                .notes(journey.getNotes())
                .version(journey.getVersion())
                .dateSource(journey.getDateSource())
                .dateConfidence(journey.getDateConfidence())
                .gestationalDatingBasis(journey.getGestationalDatingBasis())
                .gestationalDatingRevision(journey.getGestationalDatingRevision())
                .gestationalDatingEffectiveAt(journey.getGestationalDatingEffectiveAt())
                .gestationalDatingQuarantineReasonCode(
                        journey.getGestationalDatingQuarantineReasonCode())
                .canonicalLmp(dating.canonicalLmp())
                .completedGestationalWeek(dating.resolved()
                        ? dating.completedGestationalWeek() : null)
                .sourceWeekNumber(dating.resolved() ? dating.sourceWeekNumber() : null)
                .plan(dating.plan())
                .createdAt(journey.getCreatedAt())
                .build();
    }

    private JourneyResponse toJourneyResponse(MotherJourney journey) {
        GestationalDatingResolution dating = responseDating(journey);
        return JourneyResponse.builder()
                .journeyId(journey.getId())
                .ownerUserId(journey.getOwnerUserId())
                .journeyType(journey.getJourneyType().name())
                .startDate(journey.getStartDate())
                .lastMenstrualDate(journey.getLastMenstrualDate())
                .estimatedDueDate(journey.getEstimatedDueDate())
                .deliveryDate(journey.getDeliveryDate())
                .pregnancyOutcome(journey.getPregnancyOutcome())
                .pregnancyOutcomeDate(journey.getPregnancyOutcomeDate())
                .status(journey.getStatus().name())
                .notes(journey.getNotes())
                .version(journey.getVersion())
                .dateSource(journey.getDateSource())
                .dateConfidence(journey.getDateConfidence())
                .gestationalDatingBasis(journey.getGestationalDatingBasis())
                .gestationalDatingRevision(journey.getGestationalDatingRevision())
                .gestationalDatingEffectiveAt(journey.getGestationalDatingEffectiveAt())
                .gestationalDatingQuarantineReasonCode(
                        journey.getGestationalDatingQuarantineReasonCode())
                .canonicalLmp(dating.canonicalLmp())
                .completedGestationalWeek(dating.resolved()
                        ? dating.completedGestationalWeek() : null)
                .sourceWeekNumber(dating.resolved() ? dating.sourceWeekNumber() : null)
                .plan(dating.plan())
                .createdAt(journey.getCreatedAt())
                .updatedAt(journey.getUpdatedAt())
                .build();
    }

    private GestationalDatingResolution responseDating(MotherJourney journey) {
        if (!GestationalDatingResolver.hasResolvedAuthority(journey)) {
            return GestationalDatingResolution.unresolved(
                    journey.getLastMenstrualDate(), journey.getEstimatedDueDate(), false);
        }
        LocalDate today = LocalDate.now(clock.withZone(CAREBRIDGE_BUSINESS_ZONE));
        LocalDate canonical = GestationalDatingResolver.canonicalLmp(
                journey.getGestationalDatingBasis(),
                journey.getLastMenstrualDate(),
                journey.getEstimatedDueDate());
        if (canonical == null || canonical.isAfter(today)) {
            return GestationalDatingResolution.unresolved(
                    journey.getLastMenstrualDate(), journey.getEstimatedDueDate(), false);
        }
        int completed = GestationalDatingResolver.completedGestationalWeek(canonical, today);
        int source = GestationalDatingResolver.sourceWeekNumber(completed);
        return new GestationalDatingResolution(
                journey.getGestationalDatingBasis(),
                journey.getLastMenstrualDate(),
                journey.getEstimatedDueDate(),
                canonical,
                true,
                true,
                false,
                completed,
                source,
                GestationalDatingResolver.planForSourceWeek(source));
    }

    private JourneyTransitionResponse toTransitionResponse(MotherJourneyTransition transition) {
        return JourneyTransitionResponse.builder()
                .transitionId(transition.getId())
                .eventType(transition.getEventType())
                .fromStage(transition.getFromStage())
                .toStage(transition.getToStage())
                .changedFields(List.copyOf(transition.getChanges().keySet()))
                .source(transition.getSource())
                .confidence(transition.getConfidence())
                .reason(transition.getReason())
                .effectiveAt(transition.getEffectiveAt())
                .recordedAt(transition.getRecordedAt())
                .journeyVersion(transition.getJourneyVersion())
                .gestationalDatingBasis(transition.getGestationalDatingBasis())
                .gestationalDatingRevision(transition.getGestationalDatingRevision())
                .canonicalLmp(transition.getCanonicalLmp())
                .build();
    }
}
