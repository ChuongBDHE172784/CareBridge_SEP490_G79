package com.carebridge.backend.reminder;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.RequiredAuditEvent;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationScheduleService;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.ReminderActionAuditContext;
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
    @Mock private AppointmentNotificationScheduleService appointmentNotificationScheduleService;
    @InjectMocks private ReminderServiceImpl reminderService;

    /**
     * Reminder status mutations audit through the transactional {@code logRequired} contract
     * (RequiredAuditEvent), not the fire-and-forget {@code log} overload.
     */
    private void verifyRequiredAudit(AuditAction action, ReminderStatus before, ReminderStatus after) {
        ArgumentCaptor<RequiredAuditEvent> captor = ArgumentCaptor.forClass(RequiredAuditEvent.class);
        verify(auditService).logRequired(captor.capture());
        RequiredAuditEvent event = captor.getValue();
        assertThat(event.action()).isEqualTo(action);
        assertThat(event.actorUserId()).isEqualTo(ReminderTestFactory.OWNER_ID);
        assertThat(event.subjectUserId()).isEqualTo(ReminderTestFactory.OWNER_ID);
        assertThat(event.resourceType()).isEqualTo("ReminderOccurrence");
        assertThat(event.beforePayload()).containsEntry("status", before.name());
        assertThat(event.afterPayload()).containsEntry("status", after.name());
    }

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

    @Test
    void updateReminder_recurrenceEndDate_returnsUpdatedEndDate() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        Instant endDate = Instant.now().plus(7, ChronoUnit.DAYS);
        var request = new com.carebridge.backend.reminder.dto.UpdateReminderRequest();
        request.setRecurrenceType(RecurrenceType.DAILY);
        request.setRecurrenceEndDate(endDate);
        request.setRecurrenceEndDateSet(true);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID, request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getRecurrenceType()).isEqualTo(RecurrenceType.DAILY.name());
        assertThat(resp.getRecurrenceEndDate()).isEqualTo(endDate);
    }

    @Test
    void updateReminder_recurrenceEndDateSetWithNull_clearsEndDate() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        pending.setRecurrenceType(RecurrenceType.DAILY);
        pending.setRecurrenceEndDate(Instant.now().plus(7, ChronoUnit.DAYS));
        var request = new com.carebridge.backend.reminder.dto.UpdateReminderRequest();
        request.setRecurrenceEndDateSet(true);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID, request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getRecurrenceEndDate()).isNull();
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

    @Test
    void snoozeReminder_appointmentKeepsDurableMilestonesAndSchedulesTransientFollowUp() {
        var appointment = ReminderTestFactory.pendingReminder(ReminderType.APPOINTMENT);
        Instant scheduledAt = appointment.getScheduledAt();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(appointment));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.scheduleFcmPush(any(), any(), any(), any()))
                .thenReturn("appointment-snooze-job");

        ReminderDetailResponse response = reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validSnoozeRequest(),
                ReminderTestFactory.OWNER_ID);

        assertThat(response.getStatus()).isEqualTo(ReminderStatus.SNOOZED.name());
        assertThat(response.getScheduledAt()).isEqualTo(scheduledAt);
        verify(appointmentNotificationScheduleService, never()).cancelRemaining(any());
        verify(appointmentNotificationScheduleService, never()).reschedule(any(), any(), anyBoolean(), any());
        verify(notificationService).scheduleFcmPush(
                eq(ReminderTestFactory.OWNER_ID),
                eq(appointment.getTitle()),
                contains("Snoozed reminder"),
                eq(response.getSnoozedUntil()));
    }

    @Test
    void snoozeReminder_appointmentResnoozeCancelsOnlyPreviousTransientJob() {
        var appointment = ReminderTestFactory.pendingReminder(ReminderType.APPOINTMENT);
        appointment.setStatus(ReminderStatus.SNOOZED);
        appointment.setFcmJobId("previous-appointment-snooze-job");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(appointment));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.scheduleFcmPush(any(), any(), any(), any()))
                .thenReturn("replacement-appointment-snooze-job");

        reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validSnoozeRequest(),
                ReminderTestFactory.OWNER_ID);

        verify(notificationService).cancelFcmJob("previous-appointment-snooze-job");
        verify(appointmentNotificationScheduleService, never()).cancelRemaining(any());
        assertThat(appointment.getFcmJobId()).isEqualTo("replacement-appointment-snooze-job");
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

    @Test
    void snoozeReminder_recurringSkippedSeries_returnsSnoozedStatus() {
        var recurring = ReminderTestFactory.skippedReminder();
        recurring.setRecurrenceType(RecurrenceType.DAILY);
        recurring.setRecurrenceEndDate(Instant.now().plus(3, ChronoUnit.DAYS));
        recurring.setFcmJobId("job-recurring-snooze");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validSnoozeRequest(),
                ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SNOOZED.name());
        assertThat(resp.getSnoozedUntil()).isNotNull();
        verify(notificationService).cancelFcmJob("job-recurring-snooze");
        verify(notificationService).scheduleFcmPush(
                eq(ReminderTestFactory.OWNER_ID), anyString(), anyString(), any());
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
        verifyRequiredAudit(AuditAction.REMINDER_COMPLETED, ReminderStatus.PENDING, ReminderStatus.COMPLETED);
    }

    @Test
    void completeReminder_snoozedAppointmentCancelsMilestonesAndTransientJob() {
        var appointment = ReminderTestFactory.pendingReminder(ReminderType.APPOINTMENT);
        appointment.setStatus(ReminderStatus.SNOOZED);
        appointment.setFcmJobId("appointment-snooze-job");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(appointment));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse response = reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(response.getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
        verify(appointmentNotificationScheduleService).cancelRemaining(ReminderTestFactory.REMINDER_ID);
        verify(notificationService).cancelFcmJob("appointment-snooze-job");
        assertThat(appointment.getFcmJobId()).isNull();
    }

    @Test
    void completeReminder_recurringAppointmentCancelsOnlySelectedOccurrence() {
        var appointment = ReminderTestFactory.pendingReminder(ReminderType.APPOINTMENT);
        appointment.setRecurrenceType(RecurrenceType.DAILY);
        UUID occurrenceId = UUID.fromString("00000000-0000-0000-0000-000000000301");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(appointment));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.OWNER_ID,
                new ReminderActionAuditContext(occurrenceId, "USER_ACTION", UUID.randomUUID()));

        verify(appointmentNotificationScheduleService).cancelOccurrence(
                ReminderTestFactory.REMINDER_ID, occurrenceId);
        verify(appointmentNotificationScheduleService, never()).cancelRemaining(any());
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
        verifyRequiredAudit(AuditAction.REMINDER_COMPLETED, ReminderStatus.SNOOZED, ReminderStatus.COMPLETED);
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
        verifyRequiredAudit(AuditAction.REMINDER_SKIPPED, ReminderStatus.PENDING, ReminderStatus.SKIPPED);
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
        verifyRequiredAudit(AuditAction.REMINDER_SKIPPED, ReminderStatus.SNOOZED, ReminderStatus.SKIPPED);
    }

    @Test
    void skipReminder_dailyRecurringReminder_doesNotCreatePersistedOccurrence() {
        Instant scheduledAt = Instant.now().minus(1, ChronoUnit.HOURS);
        var recurring = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        recurring.setScheduledAt(scheduledAt);
        recurring.setRecurrenceType(RecurrenceType.DAILY);
        recurring.setRecurrenceEndDate(scheduledAt.plus(3, ChronoUnit.DAYS));
        recurring.setFcmJobId("job-recurring-skip");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
        verify(reminderRepository, times(1)).save(any(Reminder.class));
        verify(notificationService).cancelFcmJob("job-recurring-skip");
        verify(notificationService, never()).scheduleFcmPush(any(), anyString(), anyString(), any());
        verifyRequiredAudit(AuditAction.REMINDER_SKIPPED, ReminderStatus.PENDING, ReminderStatus.SKIPPED);
        verify(auditService, never()).logRequired(argThat(e -> e.action() == AuditAction.REMINDER_CREATED));
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
        verifyRequiredAudit(AuditAction.REMINDER_SKIPPED, ReminderStatus.PENDING, ReminderStatus.SKIPPED);
        verify(auditService, never()).logRequired(argThat(e -> e.action() == AuditAction.REMINDER_CREATED));
    }

    @Test
    void completeReminder_recurringSkippedSeries_returnsCompletedStatus() {
        var recurring = ReminderTestFactory.skippedReminder();
        recurring.setRecurrenceType(RecurrenceType.DAILY);
        recurring.setRecurrenceEndDate(Instant.now().plus(3, ChronoUnit.DAYS));
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
        verifyRequiredAudit(AuditAction.REMINDER_COMPLETED, ReminderStatus.SKIPPED, ReminderStatus.COMPLETED);
    }

    @Test
    void skipReminder_recurringCompletedSeries_returnsSkippedStatus() {
        var recurring = ReminderTestFactory.completedReminder();
        recurring.setRecurrenceType(RecurrenceType.DAILY);
        recurring.setRecurrenceEndDate(Instant.now().plus(3, ChronoUnit.DAYS));
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(recurring));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
        verifyRequiredAudit(AuditAction.REMINDER_SKIPPED, ReminderStatus.COMPLETED, ReminderStatus.SKIPPED);
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
    void deleteReminder_completedReminder_softDeletesAndAudits() {
        var completed = ReminderTestFactory.completedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(completed));

        reminderService.deleteReminder(ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(completed.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(completed);
        verify(auditService).log(AuditAction.REMINDER_CANCELLED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "cancelled");
    }

    @Test
    void deleteReminder_skippedReminder_softDeletesAndAudits() {
        var skipped = ReminderTestFactory.skippedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(skipped));

        reminderService.deleteReminder(ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(skipped.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(skipped);
        verify(auditService).log(AuditAction.REMINDER_CANCELLED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "cancelled");
    }

    @Test
    void enableReminder_cancelledReminder_returnsPendingAndSchedulesNotification() {
        var cancelled = ReminderTestFactory.cancelledReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(cancelled));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationService.scheduleFcmPush(
                eq(ReminderTestFactory.OWNER_ID), anyString(), anyString(), eq(cancelled.getScheduledAt())))
                .thenReturn("job-enabled");

        ReminderDetailResponse resp = reminderService.enableReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.PENDING.name());
        assertThat(cancelled.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(cancelled.getFcmJobId()).isEqualTo("job-enabled");
        verify(reminderRepository).save(cancelled);
        verify(auditService).log(AuditAction.REMINDER_CREATED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "enabled");
    }

    @Test
    void enableReminder_pendingReminder_returnsExistingWithoutSaving() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        ReminderDetailResponse resp = reminderService.enableReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.PENDING.name());
        verify(reminderRepository, never()).save(any());
        verify(notificationService, never()).scheduleFcmPush(any(), anyString(), anyString(), any());
    }

    @Test
    void hardDeleteReminder_ownedReminder_deletesFromRepositoryAndAudits() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        pending.setFcmJobId("job-hard-delete");
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        reminderService.hardDeleteReminder(ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        verify(notificationService).cancelFcmJob("job-hard-delete");
        verify(reminderRepository).delete(pending);
        verify(reminderRepository, never()).save(any());
        verify(auditService).log(AuditAction.REMINDER_CANCELLED, ReminderTestFactory.OWNER_ID,
                "Reminder", ReminderTestFactory.REMINDER_ID.toString(), "permanently deleted");
    }

    @Test
    void hardDeleteReminder_notOwner_throwsRem016() {
        var other = ReminderTestFactory.reminderOwnedByOther(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> reminderService.hardDeleteReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("REM-016");
                });
        verify(reminderRepository, never()).delete(any());
    }

    @Test
    void hardDeleteReminder_notFound_throwsRem015() {
        when(reminderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.hardDeleteReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("REM-015");
                });
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
