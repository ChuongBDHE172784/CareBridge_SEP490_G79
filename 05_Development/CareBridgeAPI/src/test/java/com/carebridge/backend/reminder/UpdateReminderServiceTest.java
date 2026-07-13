package com.carebridge.backend.reminder;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.impl.ReminderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC48 — Update / Snooze / Complete / Skip Reminder service tests.
 * TDD Red Phase: all tests must FAIL until Green implementation.
 */
@ExtendWith(MockitoExtension.class)
class UpdateReminderServiceTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private INotificationService notificationService;
    @Mock private AuditService auditService;
    @InjectMocks private ReminderServiceImpl reminderService;

    // UPD-TC-001: Happy path — update title and scheduledAt
    @Test
    void updateReminder_validRequest_returnsUpdatedReminder() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenReturn(pending);
        var request = ReminderTestFactory.validUpdateRequest();

        ReminderDetailResponse resp = reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID, request, ReminderTestFactory.OWNER_ID);

        assertThat(resp).isNotNull();
    }

    // UPD-TC-002: Not found → REM-006 / 404
    @Test
    void updateReminder_notFound_throwsBusinessException404() {
        when(reminderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // UPD-TC-003: Ownership check — non-owner → REM-004 / 403
    @Test
    void updateReminder_notOwner_throwsBusinessException403() {
        var other = ReminderTestFactory.reminderOwnedByOther(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // UPD-TC-004: ADR-REM-STATE-001 — COMPLETED is terminal; update blocked → 409
    @Test
    void updateReminder_completedReminder_throwsBusinessException409() {
        var completed = ReminderTestFactory.completedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("REM-007");
                });
    }

    // UPD-TC-005: ADR-REM-STATE-001 — SKIPPED is terminal; update blocked → 409
    @Test
    void updateReminder_skippedReminder_throwsBusinessException409() {
        var skipped = ReminderTestFactory.skippedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(skipped));

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // UPD-TC-006: Snooze happy path — PENDING → SNOOZED
    @Test
    void snoozeReminder_pendingReminder_returnsSnoozedStatus() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validSnoozeRequest(),
                ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SNOOZED.name());
        assertThat(resp.getSnoozedUntil()).isNotNull();
    }

    // UPD-TC-007: Snooze — snoozedUntil > 24h in future → REM-008 / 400
    @Test
    void snoozeReminder_snoozedUntilTooFar_throwsBusinessException400() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.snoozeTooFarRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("REM-008");
                });
    }

    // UPD-TC-008: Snooze — snoozedUntil in past → REM-005 / 400
    @Test
    void snoozeReminder_snoozedUntilInPast_throwsBusinessException400() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.snoozeInPastRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // UPD-TC-009: Complete reminder happy path → status = COMPLETED
    @Test
    void completeReminder_pendingReminder_returnsCompletedStatus() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        pending.setFcmJobId("job-complete");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
        verify(notificationService).cancelFcmJob("job-complete");
        verify(auditService).log(AuditAction.REMINDER_COMPLETED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "completed");
    }

    // UPD-TC-010: Complete snoozed reminder happy path → status = COMPLETED
    // UPD-TC-011: Skip reminder happy path → status = SKIPPED
    @Test
    void completeReminder_snoozedReminder_returnsCompletedStatus() {
        var snoozed = ReminderTestFactory.snoozedReminder();
        snoozed.setFcmJobId("job-snoozed-complete");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(snoozed));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
        verify(notificationService).cancelFcmJob("job-snoozed-complete");
        verify(auditService).log(AuditAction.REMINDER_COMPLETED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "completed");
    }

    @Test
    void skipReminder_pendingReminder_returnsSkippedStatus() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        pending.setFcmJobId("job-skip");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
        verify(notificationService).cancelFcmJob("job-skip");
        verify(auditService).log(AuditAction.REMINDER_SKIPPED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "skipped");
    }

    // UPD-TC-012: Skip snoozed reminder happy path → status = SKIPPED
    // UPD-TC-013: Complete — COMPLETED is terminal; double-complete → 409
    @Test
    void skipReminder_snoozedReminder_returnsSkippedStatus() {
        var snoozed = ReminderTestFactory.snoozedReminder();
        snoozed.setFcmJobId("job-snoozed-skip");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(snoozed));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
        verify(notificationService).cancelFcmJob("job-snoozed-skip");
        verify(auditService).log(AuditAction.REMINDER_SKIPPED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "skipped");
    }

    @Test
    void skipReminder_dailyRecurringReminder_createsNextPendingOccurrence() {
        Instant scheduledAt = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant nextScheduledAt = scheduledAt.plus(1, ChronoUnit.DAYS);
        UUID nextReminderId = UUID.randomUUID();
        var recurring = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        recurring.setScheduledAt(scheduledAt);
        recurring.setRecurrenceType(RecurrenceType.DAILY);
        recurring.setRecurrenceEndDate(scheduledAt.plus(3, ChronoUnit.DAYS));
        recurring.setFcmJobId("job-recurring-skip");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> {
            Reminder saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(nextReminderId);
            }
            return saved;
        });
        when(notificationService.scheduleFcmPush(
                ReminderTestFactory.OWNER_ID,
                recurring.getTitle(),
                "Reminder: " + recurring.getTitle(),
                nextScheduledAt))
                .thenReturn("job-next-occurrence");

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
        ArgumentCaptor<Reminder> savedCaptor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, times(3)).save(savedCaptor.capture());
        List<Reminder> savedReminders = savedCaptor.getAllValues();
        assertThat(savedReminders.get(0).getId()).isEqualTo(ReminderTestFactory.REMINDER_ID);
        assertThat(savedReminders.get(0).getStatus()).isEqualTo(ReminderStatus.SKIPPED);
        assertThat(savedReminders.get(1).getId()).isEqualTo(nextReminderId);
        assertThat(savedReminders.get(1).getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(savedReminders.get(1).getScheduledAt()).isEqualTo(nextScheduledAt);
        assertThat(savedReminders.get(1).getRecurrenceType()).isEqualTo(RecurrenceType.DAILY);
        assertThat(savedReminders.get(1).getRecurrenceEndDate()).isEqualTo(recurring.getRecurrenceEndDate());
        assertThat(savedReminders.get(2).getFcmJobId()).isEqualTo("job-next-occurrence");
        verify(notificationService).cancelFcmJob("job-recurring-skip");
        verify(notificationService).scheduleFcmPush(
                ReminderTestFactory.OWNER_ID,
                recurring.getTitle(),
                "Reminder: " + recurring.getTitle(),
                nextScheduledAt);
        verify(auditService).log(AuditAction.REMINDER_CREATED, ReminderTestFactory.OWNER_ID,
                "Reminder", nextReminderId.toString(), "next recurring occurrence created after skip");
        verify(auditService).log(AuditAction.REMINDER_SKIPPED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "skipped");
    }

    @Test
    void skipReminder_dailyRecurringReminderPastEndDate_doesNotCreateNextOccurrence() {
        Instant scheduledAt = Instant.now().minus(1, ChronoUnit.HOURS);
        var recurring = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        recurring.setScheduledAt(scheduledAt);
        recurring.setRecurrenceType(RecurrenceType.DAILY);
        recurring.setRecurrenceEndDate(scheduledAt.plus(12, ChronoUnit.HOURS));
        recurring.setFcmJobId("job-recurring-ended");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
        verify(reminderRepository, times(1)).save(any(Reminder.class));
        verify(notificationService).cancelFcmJob("job-recurring-ended");
        verify(notificationService, never()).scheduleFcmPush(any(), anyString(), anyString(), any());
        verify(auditService).log(AuditAction.REMINDER_SKIPPED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "skipped");
        verify(auditService, never()).log(eq(AuditAction.REMINDER_CREATED), any(), anyString(), anyString(), anyString());
    }

    @Test
    void completeReminder_alreadyCompleted_throwsBusinessException409() {
        var completed = ReminderTestFactory.completedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void skipReminder_alreadyCancelled_throwsRem011() {
        var cancelled = ReminderTestFactory.cancelledReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("REM-011");
                });
    }

    @Test
    void deleteReminder_pendingReminder_softDeletesAndAudits() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        pending.setFcmJobId("job-delete");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        reminderService.deleteReminder(ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(pending);
        verify(reminderRepository, never()).delete(any());
        verify(notificationService).cancelFcmJob("job-delete");
        verify(auditService).log(AuditAction.REMINDER_CANCELLED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "cancelled");
    }

    @Test
    void deleteReminder_alreadyCancelled_isIdempotentNoOp() {
        var cancelled = ReminderTestFactory.cancelledReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(cancelled));

        reminderService.deleteReminder(ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        verify(reminderRepository, never()).save(any());
        verify(notificationService, never()).cancelFcmJob(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteReminder_completedReminder_throwsRem017() {
        var completed = ReminderTestFactory.completedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> reminderService.deleteReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("REM-017");
                });
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void deleteReminder_notOwner_throwsRem016() {
        var other = ReminderTestFactory.reminderOwnedByOther(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> reminderService.deleteReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("REM-016");
                });
    }

    @Test
    void deleteReminder_notFound_throwsRem015() {
        when(reminderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.deleteReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("REM-015");
                });
    }
}
