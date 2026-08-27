package com.carebridge.backend.checklist.distribution.job;

import com.carebridge.backend.checklist.distribution.ChecklistHistoryReconciliationService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyTransitionType;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.event.MotherJourneyTransitioned;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Closes pregnancy checklist work immediately after an authoritative outcome or
 * terminal pregnancy transition.  The scheduler remains the repair backstop;
 * this listener makes the normal outcome flow deterministic without waiting for
 * the next sweep or a subsequent app open.
 */
@Component
public class ChecklistJourneyTransitionListener {

    private static final Logger log = LoggerFactory.getLogger(ChecklistJourneyTransitionListener.class);
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MotherJourneyRepository journeyRepository;
    private final ChecklistHistoryReconciliationService historyReconciliationService;
    private final Clock clock;

    @Autowired
    public ChecklistJourneyTransitionListener(
            MotherJourneyRepository journeyRepository,
            ChecklistHistoryReconciliationService historyReconciliationService) {
        this(journeyRepository, historyReconciliationService, Clock.systemUTC());
    }

    ChecklistJourneyTransitionListener(
            MotherJourneyRepository journeyRepository,
            ChecklistHistoryReconciliationService historyReconciliationService,
            Clock clock) {
        this.journeyRepository = journeyRepository;
        this.historyReconciliationService = historyReconciliationService;
        this.clock = clock;
    }

    @EventListener
    public void onJourneyTransitioned(MotherJourneyTransitioned event) {
        if (event == null || event.ownerUserId() == null || event.journeyId() == null
                || !closesPregnancyScope(event)) {
            return;
        }
        UUID correlationId = event.correlationId() == null ? UUID.randomUUID() : event.correlationId();
        ZoneId zone = resolveZone();
        try {
            MotherJourney journey = journeyRepository.findById(event.journeyId()).orElse(null);
            if (journey != null && journey.getBaselineTimeZone() != null) {
                zone = resolveZone(journey.getBaselineTimeZone());
            }
            Instant occurredAt = event.occurredAt() == null ? clock.instant() : event.occurredAt();
            LocalDate effectiveDate = LocalDate.ofInstant(occurredAt, zone);
            historyReconciliationService.reconcile(event.ownerUserId(), effectiveDate, zone, correlationId);
        } catch (RuntimeException exception) {
            // A post-commit repair failure must not turn a successful clinical
            // outcome into an API error.  The scheduled sweep retries it.
            log.warn("checklist_outcome_history_repair_failed journeyId={} owner={} reason={}",
                    event.journeyId(), event.ownerUserId(), exception.getClass().getSimpleName());
        }
    }

    private static boolean closesPregnancyScope(MotherJourneyTransitioned event) {
        boolean outcomeToPostpartum = (event.eventType() == JourneyTransitionType.OUTCOME_RECORDED
                || event.eventType() == JourneyTransitionType.OUTCOME_CORRECTED)
                && event.journeyType() == JourneyType.POSTPARTUM;
        boolean completedPregnancy = event.journeyType() == JourneyType.PREGNANCY
                && event.status() == JourneyStatus.COMPLETED;
        return outcomeToPostpartum || completedPregnancy;
    }

    private ZoneId resolveZone() {
        return FALLBACK_ZONE;
    }

    private ZoneId resolveZone(String value) {
        try {
            return value == null || value.isBlank() ? resolveZone() : ZoneId.of(value.trim());
        } catch (DateTimeException ignored) {
            return resolveZone();
        }
    }
}
