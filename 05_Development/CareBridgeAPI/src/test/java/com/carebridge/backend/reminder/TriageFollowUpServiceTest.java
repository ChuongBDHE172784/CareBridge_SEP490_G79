package com.carebridge.backend.reminder;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.policy.TriageFollowUpTitlePolicy;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.impl.TriageFollowUpService;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.event.IntakeSessionCompleted;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.BABY_1;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.JOURNEY_1;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.MOTHER_A;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.MOTHER_B;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.SESSION_1;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.fixedClock;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.makeEvent;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.makeYellowEvent;
import static com.carebridge.backend.reminder.TriageFollowUpTestFactory.makeYellowSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CB-TYFU-TDD-001 — TYFU-TC-01 / 04 / 07 / 08 / 11 / 12 / 13(unit).
 * Oracles per TC: roadmap Part III.3, baseline DDL :1587-1610, ADR-TYFU-001/003/004/005/006,
 * ReminderServiceImpl.java:48-76 (save → push → audit convention), AuditAction.REMINDER_CREATED.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TriageFollowUpServiceTest {

    private static final String FEVER_TITLE = "Kiểm tra lại thân nhiệt của bé";
    private static final UUID CARE_ITEM_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    @Mock private ReminderRepository reminderRepository;
    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private INotificationService notificationService;
    @Mock private AuditService auditService;

    /** Fresh service per test — Props Isolation (real pure policy, fixed Clock FX-001). */
    private TriageFollowUpService makeService(long delayHours) {
        return new TriageFollowUpService(reminderRepository, intakeSessionRepository,
                new TriageFollowUpTitlePolicy(), notificationService, auditService,
                delayHours, fixedClock());
    }

    private void wireHappyPathMocks() {
        when(reminderRepository.existsByReminderTypeAndSourceReferenceId(
                ReminderType.TRIAGE_FOLLOW_UP, SESSION_1)).thenReturn(false);
        when(intakeSessionRepository.findById(SESSION_1))
                .thenReturn(Optional.of(makeYellowSession())); // FX-005
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder reminder = invocation.getArgument(0);
            if (reminder.getId() == null) {
                reminder.setId(CARE_ITEM_ID);
            }
            return reminder;
        });
        when(notificationService.scheduleFcmPush(any(), anyString(), anyString(), any()))
                .thenReturn("fcm-job-tyfu-1");
    }

    // ── TYFU-TC-01 — YELLOW creates exactly one PENDING follow-up ───────────────

    @Test
    void tyfuTc01_yellowCompletion_createsOnePendingFollowUpWithMandatedFields() {
        wireHappyPathMocks();
        TriageFollowUpService service = makeService(4);
        IntakeSessionCompleted event = makeYellowEvent(); // FX-002

        Optional<UUID> result = service.scheduleFollowUp(event);

        assertThat(result).isPresent();
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        Reminder saved = captor.getValue();
        assertThat(saved.getOwnerUserId()).isEqualTo(MOTHER_A);                       // ADR-TYFU-001
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.TRIAGE_FOLLOW_UP); // ADR-TYFU-001
        assertThat(saved.getScheduledAt())
                .isEqualTo(Instant.parse("2026-07-26T14:00:00Z"));                    // ADR-TYFU-005: completedAt + 4h
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);              // TDS §6.3 invariant 1
        assertThat(saved.getSourceReferenceType()).isEqualTo("TRIAGE_SESSION");       // ADR-TYFU-003
        assertThat(saved.getSourceReferenceId()).isEqualTo(SESSION_1);                // ADR-TYFU-003
        assertThat(saved.getJourneyId()).isEqualTo(JOURNEY_1);                        // TDS §5.2 row table
        assertThat(saved.getBabyId()).isEqualTo(BABY_1);                              // TDS §5.2 row table
        assertThat(saved.getTitle()).isEqualTo(FEVER_TITLE);                          // ADR-TYFU-006 row 1
        assertThat(saved.getRecurrenceType()).isEqualTo(RecurrenceType.NONE);         // one-shot
        assertThat(saved.getFcmJobId()).isEqualTo("fcm-job-tyfu-1");                  // ADR-TYFU-004

        verify(notificationService, times(1)).scheduleFcmPush(
                eq(MOTHER_A), eq(FEVER_TITLE), anyString(),
                eq(Instant.parse("2026-07-26T14:00:00Z")));
        verify(auditService, times(1)).log(eq(AuditAction.REMINDER_CREATED), eq(MOTHER_A),
                eq("Reminder"), eq(CARE_ITEM_ID.toString()), any());
    }

    // ── TYFU-TC-04 — duplicate event (new eventId, same sessionId) idempotent ───

    @Test
    void tyfuTc04_duplicateEvent_isIdempotent_noSecondRowNoPushNoAudit() {
        when(reminderRepository.existsByReminderTypeAndSourceReferenceId(
                ReminderType.TRIAGE_FOLLOW_UP, SESSION_1)).thenReturn(true);
        TriageFollowUpService service = makeService(4);

        // FX-004 — fresh eventId, same SESSION_1 (Logic Issue L3)
        Optional<UUID> result = service.scheduleFollowUp(makeYellowEvent());

        assertThat(result).isEmpty(); // TYFU-002 — skip, not error
        verify(reminderRepository, never()).save(any());
        verify(notificationService, never()).scheduleFcmPush(any(), anyString(), anyString(), any());
        verify(auditService, never()).log(any(), any(UUID.class), anyString(), anyString(), any());
    }

    // ── TYFU-TC-07 — FCM failure: item kept, fcmJobId null (TYFU-004) ───────────

    @Test
    void tyfuTc07_fcmFailure_keepsCareItemWithNullJobId_andStillAudits() {
        wireHappyPathMocks();
        doThrow(new IllegalStateException("synthetic fcm outage"))
                .when(notificationService).scheduleFcmPush(any(), anyString(), anyString(), any());
        TriageFollowUpService service = makeService(4);

        Optional<UUID> result = service.scheduleFollowUp(makeYellowEvent());

        assertThat(result).isPresent();
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getFcmJobId()).isNull(); // ADR-TYFU-004 / UC45 ADR-REM-001
        verify(auditService, times(1)).log(eq(AuditAction.REMINDER_CREATED), eq(MOTHER_A),
                eq("Reminder"), eq(CARE_ITEM_ID.toString()), any());
    }

    // ── TYFU-TC-08 — session not found: skip, no side effects (TYFU-001) ────────

    @Test
    void tyfuTc08_sessionNotFound_skipsWithEmptyOptional_noSideEffects() {
        when(reminderRepository.existsByReminderTypeAndSourceReferenceId(
                ReminderType.TRIAGE_FOLLOW_UP, SESSION_1)).thenReturn(false);
        when(intakeSessionRepository.findById(SESSION_1)).thenReturn(Optional.empty());
        TriageFollowUpService service = makeService(4);

        assertThatCode(() -> {
            Optional<UUID> result = service.scheduleFollowUp(makeYellowEvent());
            assertThat(result).isEmpty();
        }).doesNotThrowAnyException();
        verify(reminderRepository, never()).save(any());
        verify(notificationService, never()).scheduleFcmPush(any(), anyString(), anyString(), any());
        verify(auditService, never()).log(any(), any(UUID.class), anyString(), anyString(), any());
    }

    // ── TYFU-TC-11 — delay boundaries: 1/6/24 honored; 0 and 25 → default 4 ─────

    @ParameterizedTest
    @CsvSource({
            "6,  2026-07-26T16:00:00Z",  // upper product bound (roadmap 4–6 h)
            "1,  2026-07-26T11:00:00Z",  // technical lower bound
            "24, 2026-07-27T10:00:00Z",  // technical upper bound
            "0,  2026-07-26T14:00:00Z",  // invalid → default 4 (TYFU-005)
            "25, 2026-07-26T14:00:00Z",  // invalid → default 4 (TYFU-005)
    })
    void tyfuTc11_delayConfigBoundaries_honoredOrFallBackToDefault(long delayHours, String expected) {
        wireHappyPathMocks();
        TriageFollowUpService service = makeService(delayHours); // FX-007

        service.scheduleFollowUp(makeYellowEvent());

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getScheduledAt()).isEqualTo(Instant.parse(expected));
    }

    // ── TYFU-TC-12 — Clock control: null completedAt fallback; UTC day boundary ─

    @Test
    void tyfuTc12_nullCompletedAt_fallsBackToInjectedFixedClock() {
        wireHappyPathMocks();
        TriageFollowUpService service = makeService(4);

        service.scheduleFollowUp(makeEvent(RiskLevel.YELLOW, null)); // FX-008

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        // clock.instant() + 4h — fixed Clock (FX-001), never Instant.now()
        assertThat(captor.getValue().getScheduledAt())
                .isEqualTo(Instant.parse("2026-07-26T14:00:00Z"));
    }

    @Test
    void tyfuTc12_utcDayBoundary_pureInstantArithmetic_noZoneConversion() {
        wireHappyPathMocks();
        TriageFollowUpService service = makeService(4);

        service.scheduleFollowUp(
                makeEvent(RiskLevel.YELLOW, Instant.parse("2026-07-26T23:59:59Z"))); // FX-010

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getScheduledAt())
                .isEqualTo(Instant.parse("2026-07-27T03:59:59Z"));
    }

    // ── TYFU-TC-13 (unit) — ownership: owner is the event's mother ──────────────

    @Test
    void tyfuTc13_ownerIsAlwaysEventUser_neverAnotherMother() {
        wireHappyPathMocks();
        TriageFollowUpService service = makeService(4);

        service.scheduleFollowUp(makeYellowEvent());

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getOwnerUserId())
                .isEqualTo(MOTHER_A)      // Auth Matrix TDS §16 — Own only
                .isNotEqualTo(MOTHER_B);  // CWE-639 guard
    }
}
