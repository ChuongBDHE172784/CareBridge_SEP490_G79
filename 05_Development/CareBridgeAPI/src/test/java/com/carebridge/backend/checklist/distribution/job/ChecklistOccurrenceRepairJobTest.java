package com.carebridge.backend.checklist.distribution.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.distribution.ChecklistHistoryReconciliationService;
import com.carebridge.backend.checklist.distribution.EnsureEligibleChecklistAssignmentsService;
import com.carebridge.backend.checklist.distribution.config.ChecklistRepairProperties;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistOccurrenceRepairJobTest {

    @Mock private MotherJourneyRepository journeyRepository;
    @Mock private EnsureEligibleChecklistAssignmentsService ensureAssignments;
    @Mock private ChecklistHistoryReconciliationService historyReconciliationService;

    private ChecklistRepairProperties properties;
    private ChecklistOccurrenceRepairJob job;

    @BeforeEach
    void setUp() {
        properties = new ChecklistRepairProperties();
        properties.setCatchUpWeeks(4);
        properties.setMaxJourneysPerRun(10);
        job = new ChecklistOccurrenceRepairJob(
                journeyRepository,
                ensureAssignments,
                historyReconciliationService,
                properties,
                Clock.fixed(Instant.parse("2026-08-12T01:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void repairMissedOccurrencesProcessesOnlyActivePregnancyOncePerOwnerAndUsesBaselineZone() {
        UUID owner = UUID.randomUUID();
        MotherJourney activePregnancy = journey(owner, JourneyType.PREGNANCY, JourneyStatus.ACTIVE,
                "America/New_York");
        MotherJourney duplicate = journey(owner, JourneyType.PREGNANCY, JourneyStatus.ACTIVE,
                "Asia/Ho_Chi_Minh");
        duplicate.setUpdatedAt(Instant.parse("2026-08-11T00:00:00Z"));
        activePregnancy.setUpdatedAt(Instant.parse("2026-08-12T00:00:00Z"));
        when(journeyRepository.findByStatus(JourneyStatus.ACTIVE))
                .thenReturn(List.of(
                        activePregnancy,
                        duplicate,
                        journey(UUID.randomUUID(), JourneyType.PRE_PREGNANCY, JourneyStatus.ACTIVE,
                                "Asia/Ho_Chi_Minh"),
                        journey(UUID.randomUUID(), JourneyType.PREGNANCY, JourneyStatus.COMPLETED,
                                "Asia/Ho_Chi_Minh")));

        job.repairMissedOccurrences();

        ArgumentCaptor<LocalDate> asOf = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<ZoneId> zone = ArgumentCaptor.forClass(ZoneId.class);
        verify(ensureAssignments).ensureCatchUpAssignments(
                eq(owner), asOf.capture(), zone.capture(), any(UUID.class), eq(4));
        verify(ensureAssignments).ensureEligibleAssignments(
                eq(owner), eq(LocalDate.of(2026, 8, 11)), eq(ZoneId.of("America/New_York")),
                any(UUID.class));
        verify(historyReconciliationService).reconcile(
                eq(owner), eq(LocalDate.of(2026, 8, 11)), eq(ZoneId.of("America/New_York")),
                any(UUID.class));
        org.assertj.core.api.Assertions.assertThat(asOf.getValue())
                .isEqualTo(LocalDate.of(2026, 8, 11));
        org.assertj.core.api.Assertions.assertThat(zone.getValue())
                .isEqualTo(ZoneId.of("America/New_York"));
    }

    @Test
    void repairMissedOccurrencesIsolatesStageAndOwnerFailures() {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        when(journeyRepository.findByStatus(JourneyStatus.ACTIVE))
                .thenReturn(List.of(
                        journey(firstOwner, JourneyType.PREGNANCY, JourneyStatus.ACTIVE, "bad/zone"),
                        journey(secondOwner, JourneyType.PREGNANCY, JourneyStatus.ACTIVE,
                                "Asia/Ho_Chi_Minh")));
        doAnswer(invocation -> {
            if (firstOwner.equals(invocation.getArgument(0))) {
                throw new IllegalStateException("synthetic catch-up failure");
            }
            return null;
        }).when(ensureAssignments).ensureCatchUpAssignments(any(), any(), any(), any(), anyInt());
        doAnswer(invocation -> {
            if (firstOwner.equals(invocation.getArgument(0))) {
                throw new IllegalStateException("synthetic current failure");
            }
            return null;
        }).when(ensureAssignments).ensureEligibleAssignments(any(), any(), any(), any());

        job.repairMissedOccurrences();

        verify(historyReconciliationService).reconcile(eq(firstOwner), any(), any(), any());
        verify(ensureAssignments).ensureCatchUpAssignments(
                eq(secondOwner), any(), eq(ZoneId.of("Asia/Ho_Chi_Minh")), any(), eq(4));
        verify(ensureAssignments).ensureEligibleAssignments(
                eq(secondOwner), any(), eq(ZoneId.of("Asia/Ho_Chi_Minh")), any());
        verify(historyReconciliationService).reconcile(
                eq(secondOwner), any(), eq(ZoneId.of("Asia/Ho_Chi_Minh")), any());
    }

    @Test
    void disabledRepairDoesNotScanJourneys() {
        properties.setEnabled(false);

        job.repairMissedOccurrences();

        verify(journeyRepository, never()).findByStatus(any());
    }

    private MotherJourney journey(UUID owner, JourneyType type, JourneyStatus status, String zone) {
        return MotherJourney.builder()
                .id(UUID.randomUUID())
                .ownerUserId(owner)
                .journeyType(type)
                .status(status)
                .baselineTimeZone(zone)
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-08-10T00:00:00Z"))
                .build();
    }
}
