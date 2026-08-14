package com.carebridge.backend.journey.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.dto.JourneyTransitionResponse;
import com.carebridge.backend.journey.entity.DashboardStatus;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.policy.JourneyTransitionPolicy;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.repository.PregnancyOutcomeEvidenceRepository;
import com.carebridge.backend.journey.service.IJourneyService;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.journey.service.GestationalDatingResolution;
import com.carebridge.backend.journey.service.GestationalDatingResolver;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class JourneyServiceImpl implements IJourneyService {

    private final MotherJourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final CareGroupRepository careGroupRepository;
    private final Clock clock;
    private final IJourneyTransitionService transitionService;
    private final PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository;
    private final GestationalDatingResolver datingResolver;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            CareGroupMemberRepository careGroupMemberRepository,
            CareGroupRepository careGroupRepository,
            IJourneyTransitionService transitionService,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            GestationalDatingResolver datingResolver) {
        this(
                journeyRepository,
                userRepository,
                auditService,
                careGroupMemberRepository,
                careGroupRepository,
                Clock.systemDefaultZone(),
                requireTransitionService(transitionService),
                outcomeEvidenceRepository,
                datingResolver);
    }

    /**
     * @deprecated Test-only compatibility constructor. Production wiring must
     * use the canonical transition service constructor above.
     */
    @Deprecated(forRemoval = true)
    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            CareGroupMemberRepository careGroupMemberRepository,
            CareGroupRepository careGroupRepository) {
        this(
                journeyRepository,
                userRepository,
                auditService,
                careGroupMemberRepository,
                careGroupRepository,
                Clock.systemDefaultZone(),
                null,
                null,
                new GestationalDatingResolver());
    }

    /**
     * @deprecated Test-only compatibility constructor. Production wiring must
     * use the canonical transition service constructor above.
     */
    @Deprecated(forRemoval = true)
    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            CareGroupMemberRepository careGroupMemberRepository,
            CareGroupRepository careGroupRepository,
            Clock clock) {
        this(
                journeyRepository,
                userRepository,
                auditService,
                careGroupMemberRepository,
                careGroupRepository,
                clock,
                null,
                null,
                new GestationalDatingResolver());
    }

    /**
     * @deprecated Test-only compatibility constructor. Production wiring must
     * use the canonical transition service constructor above.
     */
    @Deprecated(forRemoval = true)
    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this(journeyRepository, userRepository, auditService, null, null, Clock.systemDefaultZone(), null, null,
                new GestationalDatingResolver());
    }

    /**
     * @deprecated Test-only compatibility constructor. Production wiring must
     * use the canonical transition service constructor above.
     */
    @Deprecated(forRemoval = true)
    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            Clock clock) {
        this(journeyRepository, userRepository, auditService, null, null, clock, null, null,
                new GestationalDatingResolver());
    }

    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            IJourneyTransitionService transitionService) {
        this(journeyRepository, userRepository, auditService, null, null, Clock.systemDefaultZone(),
                requireTransitionService(transitionService), null,
                new GestationalDatingResolver());
    }

    /**
     * @deprecated Test-only compatibility constructor. Production wiring must
     * use the canonical transition service constructor above.
     */
    @Deprecated(forRemoval = true)
    public JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            Clock clock,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository) {
        this(journeyRepository, userRepository, auditService, null, null, clock, null, outcomeEvidenceRepository,
                new GestationalDatingResolver());
    }

    private JourneyServiceImpl(
            MotherJourneyRepository journeyRepository,
            UserRepository userRepository,
            AuditService auditService,
            CareGroupMemberRepository careGroupMemberRepository,
            CareGroupRepository careGroupRepository,
            Clock clock,
            IJourneyTransitionService transitionService,
            PregnancyOutcomeEvidenceRepository outcomeEvidenceRepository,
            GestationalDatingResolver datingResolver) {
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.careGroupMemberRepository = careGroupMemberRepository;
        this.careGroupRepository = careGroupRepository;
        this.clock = clock;
        this.transitionService = transitionService;
        this.outcomeEvidenceRepository = outcomeEvidenceRepository;
        this.datingResolver = datingResolver;
    }

    private static IJourneyTransitionService requireTransitionService(
            IJourneyTransitionService transitionService) {
        return Objects.requireNonNull(
                transitionService,
                "Canonical journey transition service is required for production wiring");
    }

    // ─────────────────────────────────────────────────────────────
    // UC22 — Create Mother Journey
    // ─────────────────────────────────────────────────────────────

    @Override
    public CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId) {
        if (transitionService != null) {
            return transitionService.createJourney(request, callerId);
        }
        var user = userRepository.findById(callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "JOURNEY-001",
                        "User not found: " + callerId));
        if (user.getRole() == null) {
            user.setRole(Role.MOTHER);
            userRepository.save(user);
        } else if (user.getRole() != Role.MOTHER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "JOURNEY-003",
                    "Only mother accounts can create a mother journey");
        }

        boolean exists = journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(
                callerId, request.getJourneyType(), JourneyStatus.ACTIVE);
        if (exists) {
            throw new BusinessException(HttpStatus.CONFLICT, "JOURNEY-002",
                    "An active journey of type " + request.getJourneyType() + " already exists");
        }

        LocalDate lastMenstrualDate = request.getLastMenstrualDate();
        LocalDate estimatedDueDate = request.getEstimatedDueDate();
        if (lastMenstrualDate != null && estimatedDueDate == null) {
            estimatedDueDate = lastMenstrualDate.plusDays(280);
        }

        UUID careSubjectId = ensureMotherCareSubject(callerId);
        MotherJourney journey = MotherJourney.builder()
                .ownerUserId(callerId)
                .careSubjectId(careSubjectId)
                .journeyType(request.getJourneyType())
                .startDate(request.getStartDate())
                .lastMenstrualDate(lastMenstrualDate)
                .estimatedDueDate(estimatedDueDate)
                .notes(request.getNotes())
                .status(JourneyStatus.ACTIVE)
                .build();

        MotherJourney saved = journeyRepository.saveAndFlush(journey);
        journeyRepository.linkMotherCareSubject(careSubjectId, saved.getId());

        auditService.log(AuditAction.JOURNEY_CREATED, callerId,
                "MotherJourney", saved.getId().toString(), "created");

        GestationalDatingResolution dating = datingResolver == null
                ? GestationalDatingResolution.unresolved(
                        saved.getLastMenstrualDate(), saved.getEstimatedDueDate(), false)
                : new GestationalDatingResolver().resolveUpdate(
                        saved,
                        new UpdateJourneyRequest(),
                        GestationalDatingResolver.V1,
                        LocalDate.now(clock.withZone(BUSINESS_ZONE)),
                        false);
        return CreateJourneyResponse.builder()
                .id(saved.getId())
                .journeyType(saved.getJourneyType().name())
                .status(saved.getStatus().name())
                .startDate(saved.getStartDate())
                .lastMenstrualDate(saved.getLastMenstrualDate())
                .estimatedDueDate(saved.getEstimatedDueDate())
                .notes(saved.getNotes())
                .gestationalDatingBasis(saved.getGestationalDatingBasis())
                .gestationalDatingRevision(saved.getGestationalDatingRevision())
                .gestationalDatingEffectiveAt(saved.getGestationalDatingEffectiveAt())
                .gestationalDatingQuarantineReasonCode(
                        saved.getGestationalDatingQuarantineReasonCode())
                .canonicalLmp(dating.canonicalLmp())
                .completedGestationalWeek(dating.resolved()
                        ? dating.completedGestationalWeek() : null)
                .sourceWeekNumber(dating.resolved() ? dating.sourceWeekNumber() : null)
                .plan(dating.plan())
                .createdAt(saved.getCreatedAt())
                .build();
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

    // ─────────────────────────────────────────────────────────────
    // UC23 — Update Mother Journey
    // ─────────────────────────────────────────────────────────────

    @Override
    public JourneyResponse updateJourney(UUID ownerId, UUID journeyId, UpdateJourneyRequest request) {
        if (transitionService != null) {
            return transitionService.updateJourney(ownerId, journeyId, request);
        }
        // C1 — load journey
        MotherJourney journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "JOURNEY-010",
                        "Journey not found: " + journeyId));

        // C2 — ownership check (IDOR prevention)
        if (!journey.getOwnerUserId().equals(ownerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "JOURNEY-011",
                    "Access denied: caller does not own this journey");
        }

        // C3 — only ACTIVE journeys can be modified
        if (journey.getStatus() != JourneyStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-012",
                    "Only ACTIVE journeys can be updated (current status: " + journey.getStatus() + ")");
        }

        // C4 — ARCHIVED transition is system-only; reject manual ARCHIVED requests
        if ("ARCHIVED".equalsIgnoreCase(request.getStatus())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-014",
                    "Status ARCHIVED can only be set by the system");
        }

        // A live-birth (and legacy outcome-less) journey requires a delivery date.
        // Loss and stillbirth journeys can be completed without inventing one.
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())
                && request.getDeliveryDate() == null
                && journey.getDeliveryDate() == null
                && (journey.getPregnancyOutcome() == null
                        || journey.getPregnancyOutcome()
                                == com.carebridge.backend.journey.entity.PregnancyOutcomeType.LIVE_BIRTH)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-013",
                    "deliveryDate is required when completing a journey");
        }

        // Apply field updates — only overwrite when request provides a non-null value
        MotherJourney.MotherJourneyBuilder builder = journey.toBuilder();
        if (request.getJourneyType() != null) {
            builder.journeyType(request.getJourneyType());
        }
        if (request.getNotes() != null) {
            builder.notes(request.getNotes());
        }
        if (request.getLastMenstrualDate() != null) {
            builder.lastMenstrualDate(request.getLastMenstrualDate());
            builder.estimatedDueDate(request.getLastMenstrualDate().plusDays(280));
        } else if (request.getEstimatedDueDate() != null) {
            builder.estimatedDueDate(request.getEstimatedDueDate());
            builder.lastMenstrualDate(null);
        }
        if (request.getDeliveryDate() != null) {
            builder.deliveryDate(request.getDeliveryDate());
        }
        if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
            builder.status(JourneyStatus.COMPLETED);
        }

        MotherJourney saved = journeyRepository.save(builder.build());

        auditService.log(AuditAction.JOURNEY_UPDATED, ownerId,
                "MotherJourney", saved.getId().toString(), "updated");

        return toJourneyResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JourneyTransitionPageResponse getHistory(
            UUID ownerId, UUID journeyId, Pageable pageable) {
        if (transitionService == null) {
            throw new IllegalStateException("Journey transition service is unavailable");
        }
        return transitionService.getHistory(ownerId, journeyId, pageable);
    }

    // ─────────────────────────────────────────────────────────────
    // UC24 — View Mother Journey Dashboard
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public JourneyDashboardResponse getDashboard(UUID userId) {
        var activeJourney = journeyRepository.findByOwnerUserIdAndStatusAndJourneyTypeIn(
                userId, JourneyStatus.ACTIVE, JourneyTransitionPolicy.CANONICAL_STAGES);

        if (activeJourney.isEmpty()) {
            activeJourney = journeyRepository
                    .findFirstByOwnerUserIdAndJourneyTypeAndStatusOrderByCreatedAtDesc(
                            userId, JourneyType.BABY_CARE, JourneyStatus.ACTIVE);
        }

        if (activeJourney.isEmpty() && careGroupMemberRepository != null && careGroupRepository != null) {
            var memberships = careGroupMemberRepository.findByUserIdAndInviteStatus(userId, InviteStatus.ACCEPTED);
            for (var member : memberships) {
                var groupOpt = careGroupRepository.findById(member.getCareGroupId());
                if (groupOpt.isPresent()) {
                    var motherId = groupOpt.get().getOwnerUserId();
                    activeJourney = journeyRepository.findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(
                            motherId, JourneyStatus.ACTIVE);
                    if (activeJourney.isPresent()) {
                        break;
                    }
                }
            }
        }

        // No active journey → 200 OK with NO_JOURNEY (never 404 — mobile onboarding rule)
        if (activeJourney.isEmpty()) {
            return JourneyDashboardResponse.builder()
                    .status(DashboardStatus.NO_JOURNEY)
                    .build();
        }

        MotherJourney journey = activeJourney.get();
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));

        DashboardStatus dashboardStatus = resolveDashboardStatus(journey.getJourneyType());

        Integer pregnancyWeek = null;
        Integer trimester = null;
        Long daysUntilDue = null;
        GestationalDatingBasis datingBasis = null;
        Long datingRevision = null;
        java.time.Instant datingEffectiveAt = null;
        LocalDate canonicalLmp = null;
        Integer completedGestationalWeek = null;
        Integer completedGestationalDays = null;
        Integer sourceWeekNumber = null;
        Integer plan = null;

        if (GestationalDatingResolver.hasResolvedAuthority(journey)) {
            canonicalLmp = GestationalDatingResolver.canonicalLmp(
                    journey.getGestationalDatingBasis(),
                    journey.getLastMenstrualDate(),
                    journey.getEstimatedDueDate());
            if (canonicalLmp != null && !canonicalLmp.isAfter(today)) {
                datingBasis = journey.getGestationalDatingBasis();
                datingRevision = journey.getGestationalDatingRevision();
                datingEffectiveAt = journey.getGestationalDatingEffectiveAt();
                completedGestationalWeek = GestationalDatingResolver.completedGestationalWeek(
                        canonicalLmp, today);
                completedGestationalDays = GestationalDatingResolver.completedGestationalDays(
                        canonicalLmp, today);
                sourceWeekNumber = GestationalDatingResolver.sourceWeekNumber(completedGestationalWeek);
                plan = GestationalDatingResolver.planForSourceWeek(sourceWeekNumber);
                pregnancyWeek = completedGestationalWeek;
                trimester = calculateTrimester(completedGestationalWeek);
                daysUntilDue = ChronoUnit.DAYS.between(
                        today, canonicalLmp.plusDays(GestationalDatingResolver.GESTATION_DAYS));
            }
        }

        return JourneyDashboardResponse.builder()
                .journeyId(journey.getId())
                .journeyType(journey.getJourneyType().name())
                .status(dashboardStatus)
                .pregnancyWeek(pregnancyWeek)
                .trimester(trimester)
                .daysUntilDue(daysUntilDue)
                .estimatedDueDate(datingBasis == null ? null : journey.getEstimatedDueDate())
                .lastMenstrualDate(datingBasis == null ? null : journey.getLastMenstrualDate())
                .startDate(journey.getStartDate())
                .version(journey.getVersion())
                .dateSource(journey.getDateSource())
                .dateConfidence(journey.getDateConfidence())
                .gestationalDatingBasis(datingBasis)
                .gestationalDatingRevision(datingRevision)
                .gestationalDatingEffectiveAt(datingEffectiveAt)
                .gestationalDatingQuarantineReasonCode(
                        journey.getGestationalDatingQuarantineReasonCode())
                .canonicalLmp(canonicalLmp)
                .completedGestationalWeek(completedGestationalWeek)
                .completedGestationalDays(completedGestationalDays)
                .sourceWeekNumber(sourceWeekNumber)
                .plan(plan)
                .pregnancyOutcome(journey.getPregnancyOutcome())
                .pregnancyOutcomeDate(journey.getPregnancyOutcomeDate())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private DashboardStatus resolveDashboardStatus(JourneyType type) {
        return switch (type) {
            case PREGNANCY    -> DashboardStatus.ACTIVE_PREGNANCY;
            case POSTPARTUM   -> DashboardStatus.ACTIVE_POSTPARTUM;
            case BABY_CARE    -> DashboardStatus.BABY_CARE;
            case PRE_PREGNANCY -> DashboardStatus.PRE_PREGNANCY;
        };
    }

    /** T1: weeks ≤ 13 | T2: weeks 14–26 | T3: weeks ≥ 27 */
    private int calculateTrimester(int week) {
        if (week <= 13) return 1;
        if (week <= 26) return 2;
        return 3;
    }

    private JourneyResponse toJourneyResponse(MotherJourney journey) {
        GestationalDatingResolution dating = datingResolver == null
                ? GestationalDatingResolution.unresolved(
                        journey.getLastMenstrualDate(), journey.getEstimatedDueDate(), false)
                : new GestationalDatingResolver().resolveUpdate(
                        journey,
                        new UpdateJourneyRequest(),
                        GestationalDatingResolver.V1,
                        LocalDate.now(clock.withZone(BUSINESS_ZONE)),
                        false);
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
                .gestationalDatingBasis(journey.getGestationalDatingBasis())
                .gestationalDatingRevision(journey.getGestationalDatingRevision())
                .gestationalDatingEffectiveAt(journey.getGestationalDatingEffectiveAt())
                .gestationalDatingQuarantineReasonCode(
                        journey.getGestationalDatingQuarantineReasonCode())
                .canonicalLmp(dating.canonicalLmp())
                .completedGestationalWeek(dating.resolved()
                        ? dating.completedGestationalWeek() : null)
                .completedGestationalDays(dating.resolved()
                        ? dating.completedGestationalDays() : null)
                .sourceWeekNumber(dating.resolved() ? dating.sourceWeekNumber() : null)
                .plan(dating.plan())
                .createdAt(journey.getCreatedAt())
                .updatedAt(journey.getUpdatedAt())
                .build();
    }
}
