package com.carebridge.backend.journey.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.DashboardStatus;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.journey.service.IJourneyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class JourneyServiceImpl implements IJourneyService {

    private final MotherJourneyRepository journeyRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public JourneyServiceImpl(MotherJourneyRepository journeyRepository, AuditService auditService) {
        this(journeyRepository, auditService, Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time calculations. */
    public JourneyServiceImpl(MotherJourneyRepository journeyRepository, AuditService auditService, Clock clock) {
        this.journeyRepository = journeyRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    // ─────────────────────────────────────────────────────────────
    // UC22 — Create Mother Journey
    // ─────────────────────────────────────────────────────────────

    @Override
    public CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId) {
        boolean exists = journeyRepository.existsByOwnerUserIdAndJourneyTypeAndStatus(
                callerId, request.getJourneyType(), JourneyStatus.ACTIVE);
        if (exists) {
            throw new BusinessException(HttpStatus.CONFLICT, "JOURNEY-002",
                    "An active journey of type " + request.getJourneyType() + " already exists");
        }

        MotherJourney journey = MotherJourney.builder()
                .ownerUserId(callerId)
                .journeyType(request.getJourneyType())
                .startDate(request.getStartDate())
                .estimatedDueDate(request.getEstimatedDueDate())
                .notes(request.getNotes())
                .status(JourneyStatus.ACTIVE)
                .build();

        MotherJourney saved = journeyRepository.save(journey);

        auditService.log(AuditAction.JOURNEY_CREATED, callerId,
                "MotherJourney", saved.getId().toString(), "created");

        return CreateJourneyResponse.builder()
                .id(saved.getId())
                .journeyType(saved.getJourneyType().name())
                .status(saved.getStatus().name())
                .startDate(saved.getStartDate())
                .estimatedDueDate(saved.getEstimatedDueDate())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // UC23 — Update Mother Journey
    // ─────────────────────────────────────────────────────────────

    @Override
    public JourneyResponse updateJourney(UUID ownerId, UUID journeyId, UpdateJourneyRequest request) {
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

        // C5 — COMPLETED requires a deliveryDate
        if ("COMPLETED".equalsIgnoreCase(request.getStatus()) && request.getDeliveryDate() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "JOURNEY-013",
                    "deliveryDate is required when completing a journey");
        }

        // Apply field updates — only overwrite when request provides a non-null value
        MotherJourney.MotherJourneyBuilder builder = journey.toBuilder();
        if (request.getNotes() != null) {
            builder.notes(request.getNotes());
        }
        if (request.getEstimatedDueDate() != null) {
            builder.estimatedDueDate(request.getEstimatedDueDate());
        }
        if (request.getLastMenstrualDate() != null) {
            builder.lastMenstrualDate(request.getLastMenstrualDate());
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

    // ─────────────────────────────────────────────────────────────
    // UC24 — View Mother Journey Dashboard
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public JourneyDashboardResponse getDashboard(UUID userId) {
        var activeJourney = journeyRepository.findByOwnerUserIdAndStatus(userId, JourneyStatus.ACTIVE);

        // No active journey → 200 OK with NO_JOURNEY (never 404 — mobile onboarding rule)
        if (activeJourney.isEmpty()) {
            return JourneyDashboardResponse.builder()
                    .status(DashboardStatus.NO_JOURNEY)
                    .build();
        }

        MotherJourney journey = activeJourney.get();
        LocalDate today = LocalDate.now(clock);

        DashboardStatus dashboardStatus = resolveDashboardStatus(journey.getJourneyType());

        Integer pregnancyWeek = null;
        Integer trimester = null;
        Long daysUntilDue = null;

        if (journey.getJourneyType() == JourneyType.PREGNANCY && journey.getLastMenstrualDate() != null) {
            long daysSinceLmp = ChronoUnit.DAYS.between(journey.getLastMenstrualDate(), today);
            pregnancyWeek = (int) (daysSinceLmp / 7);
            trimester = calculateTrimester(pregnancyWeek);
        }

        if (journey.getEstimatedDueDate() != null) {
            daysUntilDue = ChronoUnit.DAYS.between(today, journey.getEstimatedDueDate());
        }

        return JourneyDashboardResponse.builder()
                .journeyId(journey.getId())
                .journeyType(journey.getJourneyType().name())
                .status(dashboardStatus)
                .pregnancyWeek(pregnancyWeek)
                .trimester(trimester)
                .daysUntilDue(daysUntilDue)
                .estimatedDueDate(journey.getEstimatedDueDate())
                .lastMenstrualDate(journey.getLastMenstrualDate())
                .startDate(journey.getStartDate())
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
        return JourneyResponse.builder()
                .journeyId(journey.getId())
                .ownerUserId(journey.getOwnerUserId())
                .journeyType(journey.getJourneyType().name())
                .startDate(journey.getStartDate())
                .lastMenstrualDate(journey.getLastMenstrualDate())
                .estimatedDueDate(journey.getEstimatedDueDate())
                .deliveryDate(journey.getDeliveryDate())
                .status(journey.getStatus().name())
                .notes(journey.getNotes())
                .createdAt(journey.getCreatedAt())
                .updatedAt(journey.getUpdatedAt())
                .build();
    }
}
