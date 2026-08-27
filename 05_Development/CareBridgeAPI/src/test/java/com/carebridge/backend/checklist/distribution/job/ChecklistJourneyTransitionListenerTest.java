package com.carebridge.backend.checklist.distribution.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.distribution.ChecklistHistoryReconciliationService;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyTransitionType;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.event.MotherJourneyTransitioned;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChecklistJourneyTransitionListenerTest {

    private static final UUID JOURNEY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID CORRELATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Test
    void outcomeToPostpartumClosesPregnancyChecklistUsingJourneyZone() {
        MotherJourneyRepository journeys = Mockito.mock(MotherJourneyRepository.class);
        ChecklistHistoryReconciliationService history = Mockito.mock(ChecklistHistoryReconciliationService.class);
        when(journeys.findById(JOURNEY_ID)).thenReturn(Optional.of(MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OWNER_ID)
                .baselineTimeZone("America/New_York")
                .build()));
        var listener = new ChecklistJourneyTransitionListener(
                journeys, history, Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC));

        listener.onJourneyTransitioned(new MotherJourneyTransitioned(
                UUID.randomUUID(), JOURNEY_ID, OWNER_ID,
                JourneyTransitionType.OUTCOME_RECORDED, JourneyType.POSTPARTUM,
                JourneyStatus.ACTIVE, 4L, Instant.parse("2026-08-12T02:00:00Z"), CORRELATION_ID));

        verify(history).reconcile(eq(OWNER_ID), eq(LocalDate.of(2026, 8, 11)),
                eq(java.time.ZoneId.of("America/New_York")), eq(CORRELATION_ID));
    }

    @Test
    void unrelatedJourneyTransitionDoesNotTouchChecklistHistory() {
        MotherJourneyRepository journeys = Mockito.mock(MotherJourneyRepository.class);
        ChecklistHistoryReconciliationService history = Mockito.mock(ChecklistHistoryReconciliationService.class);
        var listener = new ChecklistJourneyTransitionListener(
                journeys, history, Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC));

        listener.onJourneyTransitioned(new MotherJourneyTransitioned(
                UUID.randomUUID(), JOURNEY_ID, OWNER_ID,
                JourneyTransitionType.DATING_CORRECTED, JourneyType.PREGNANCY,
                JourneyStatus.ACTIVE, 4L, Instant.parse("2026-08-12T02:00:00Z"), CORRELATION_ID));

        verify(history, never()).reconcile(any(), any(), any(), any());
        verify(journeys, never()).findById(any());
    }
}
