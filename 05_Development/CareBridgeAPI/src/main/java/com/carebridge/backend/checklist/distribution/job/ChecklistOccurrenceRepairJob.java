package com.carebridge.backend.checklist.distribution.job;

import com.carebridge.backend.checklist.distribution.ChecklistHistoryReconciliationService;
import com.carebridge.backend.checklist.distribution.EnsureEligibleChecklistAssignmentsService;
import com.carebridge.backend.checklist.distribution.config.ChecklistRepairProperties;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Repairs checklist occurrences when the mobile app has not opened recently.
 *
 * <p>The job has no request/session dependency: it scans the existing active maternal
 * journeys, replays a bounded weekly horizon as non-actionable catch-up evidence, and
 * then reconciles the current scope.  A failure for one stage or owner is logged and
 * isolated so the scheduler keeps making progress for every other owner.</p>
 */
@Component
public class ChecklistOccurrenceRepairJob {

    private static final Logger log = LoggerFactory.getLogger(ChecklistOccurrenceRepairJob.class);
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MotherJourneyRepository journeyRepository;
    private final EnsureEligibleChecklistAssignmentsService ensureAssignments;
    private final ChecklistHistoryReconciliationService historyReconciliationService;
    private final ChecklistRepairProperties properties;
    private final Clock clock;

    @Autowired
    public ChecklistOccurrenceRepairJob(
            MotherJourneyRepository journeyRepository,
            EnsureEligibleChecklistAssignmentsService ensureAssignments,
            ChecklistHistoryReconciliationService historyReconciliationService,
            ChecklistRepairProperties properties) {
        this(journeyRepository, ensureAssignments, historyReconciliationService, properties,
                Clock.systemUTC());
    }

    /** Visible for deterministic unit tests; production uses the system UTC clock. */
    public ChecklistOccurrenceRepairJob(
            MotherJourneyRepository journeyRepository,
            EnsureEligibleChecklistAssignmentsService ensureAssignments,
            ChecklistHistoryReconciliationService historyReconciliationService,
            ChecklistRepairProperties properties,
            Clock clock) {
        this.journeyRepository = journeyRepository;
        this.ensureAssignments = ensureAssignments;
        this.historyReconciliationService = historyReconciliationService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Runs every fifteen minutes by default.  The annotation keeps the trigger app-independent;
     * the method's guard makes an operational disablement safe without changing scheduler state.
     */
    @Scheduled(
            cron = "${carebridge.checklist.repair.cron:0 */15 * * * *}",
            zone = "${carebridge.checklist.repair.zone:Asia/Ho_Chi_Minh}")
    public void repairMissedOccurrences() {
        if (properties == null || !properties.isEnabled()) {
            return;
        }

        final List<MotherJourney> activePregnancies;
        try {
            activePregnancies = canonicalActivePregnancies();
        } catch (RuntimeException exception) {
            log.warn("checklist_repair_journey_scan_failed reason={}", failureCode(exception));
            return;
        }

        int maxJourneys = properties.boundedMaxJourneysPerRun();
        int processed = 0;
        for (MotherJourney journey : activePregnancies) {
            if (processed >= maxJourneys) {
                log.info("checklist_repair_bound_reached maxJourneys={}",
                        maxJourneys);
                break;
            }
            processed++;
            try {
                repairJourneyIsolated(journey);
            } catch (RuntimeException exception) {
                // Keep an unexpected malformed row from aborting the rest of the sweep.
                log.warn("checklist_repair_journey_failed journey={} owner={} reason={}",
                        journey.getId(), journey.getOwnerUserId(), failureCode(exception));
            }
        }
        if (processed > 0) {
            log.info("checklist_repair_completed journeys={}", processed);
        }
    }

    private List<MotherJourney> canonicalActivePregnancies() {
        List<MotherJourney> journeys = journeyRepository.findByStatus(JourneyStatus.ACTIVE);
        if (journeys == null || journeys.isEmpty()) {
            return List.of();
        }

        // A healthy database has one canonical active journey per owner.  Keeping the
        // newest row here makes the repair deterministic if a legacy/imported database has
        // duplicate active pregnancy rows, without adding a table or a repository query.
        Comparator<MotherJourney> newest = Comparator
                .comparing(MotherJourney::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MotherJourney::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MotherJourney::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        Map<UUID, MotherJourney> byOwner = journeys.stream()
                .filter(journey -> journey != null)
                .filter(journey -> journey.getStatus() == JourneyStatus.ACTIVE)
                .filter(journey -> journey.getJourneyType() == JourneyType.PREGNANCY)
                .filter(journey -> journey.getOwnerUserId() != null)
                .sorted(newest)
                .collect(Collectors.toMap(
                        MotherJourney::getOwnerUserId,
                        journey -> journey,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new));
        return List.copyOf(byOwner.values());
    }

    private void repairJourneyIsolated(MotherJourney journey) {
        UUID ownerUserId = journey.getOwnerUserId();
        UUID correlationId = UUID.randomUUID();
        ZoneId zone = resolveZone(journey.getBaselineTimeZone());
        LocalDate asOfDate = LocalDate.now(clock.withZone(zone));

        try {
            ensureAssignments.ensureCatchUpAssignments(
                    ownerUserId, asOfDate, zone, correlationId, properties.boundedCatchUpWeeks());
        } catch (RuntimeException exception) {
            log.warn("checklist_repair_catch_up_failed owner={} correlationId={} reason={}",
                    ownerUserId, correlationId, failureCode(exception));
        }

        try {
            ensureAssignments.ensureEligibleAssignments(ownerUserId, asOfDate, zone, correlationId);
        } catch (RuntimeException exception) {
            log.warn("checklist_repair_current_reconciliation_failed owner={} correlationId={} reason={}",
                    ownerUserId, correlationId, failureCode(exception));
        }

        try {
            historyReconciliationService.reconcile(ownerUserId, asOfDate, zone, correlationId);
        } catch (RuntimeException exception) {
            log.warn("checklist_repair_history_reconciliation_failed owner={} correlationId={} reason={}",
                    ownerUserId, correlationId, failureCode(exception));
        }
    }

    private ZoneId resolveZone(String journeyZone) {
        if (journeyZone != null && !journeyZone.isBlank()) {
            try {
                return ZoneId.of(journeyZone.trim());
            } catch (DateTimeException exception) {
                log.warn("checklist_repair_invalid_journey_timezone zone={} fallback={}",
                        journeyZone, configuredFallbackZone().getId());
            }
        }
        return configuredFallbackZone();
    }

    private ZoneId configuredFallbackZone() {
        if (properties != null && properties.getZone() != null) {
            return properties.getZone();
        }
        return FALLBACK_ZONE;
    }

    private static String failureCode(RuntimeException exception) {
        return exception == null ? "UNKNOWN" : exception.getClass().getSimpleName();
    }
}
