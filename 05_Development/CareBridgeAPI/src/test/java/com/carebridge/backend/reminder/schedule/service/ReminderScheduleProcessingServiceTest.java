package com.carebridge.backend.reminder.schedule.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.schedule.entity.ReminderSchedule;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderScheduleProcessingServiceTest {
    private static final UUID JOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID SCHEDULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");
    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Mock private NotificationJobRepository jobRepository;
    @Mock private ReminderScheduleRepository scheduleRepository;
    @Mock private IReminderNotificationService notificationService;
    private ReminderScheduleProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ReminderScheduleProcessingService(jobRepository, scheduleRepository,
                notificationService, Clock.fixed(NOW, ZoneOffset.UTC), 4, 10);
        org.mockito.Mockito.lenient().when(jobRepository.transitionAfterProcessing(
                any(),eq(NotificationJobType.REMINDER_SCHEDULE), any(), eq(AppointmentNotificationJobStatus.PROCESSING), any(), any(), any(), any(), any()))
                .thenReturn(1);
    }

    @Test
    void staleWorkerCannotSendAfterFencingTokenChanges() {
        NotificationJob job = job("new-worker");
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        service.process(JOB_ID, "old-worker");

        verify(notificationService, never()).sendReminderScheduleNotification(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void failedDeliveryIsRequeuedForAnotherAttempt() {
        NotificationJob job = job("worker-1");
        ReminderSchedule schedule = ReminderSchedule.builder()
                .id(SCHEDULE_ID).ownerUserId(OWNER_ID).title("Cho con bu")
                .timeZone("UTC").startDate(LocalDate.of(2026, 8, 1))
                .revision(1L).active(true).build();
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        NotificationRecordResponse failed = new NotificationRecordResponse(
                null, OWNER_ID, "REMINDER", "Cho con bu", "body", SCHEDULE_ID,
                "REMINDER_SCHEDULE", "FAILED", null, null, false, null,
                "PUSH", null, 1, null, NOW, java.util.Map.of());
        when(notificationService.sendReminderScheduleNotification(
                eq(SCHEDULE_ID), eq(JOB_ID), eq(OWNER_ID), eq("Cho con bu"),
                any(), any(), any(), any())).thenReturn(failed);

        service.process(JOB_ID, "worker-1");

        org.assertj.core.api.Assertions.assertThat(job.getStatus())
                .isEqualTo(AppointmentNotificationJobStatus.PENDING);
    }

    @Test
    void terminalTransitionFailureIsNotRetriedInsideTheAbortedTransaction() {
        NotificationJob job = job("worker-1");
        ReminderSchedule schedule = ReminderSchedule.builder()
                .id(SCHEDULE_ID).ownerUserId(OWNER_ID).title("Cho con bu")
                .timeZone("UTC").startDate(LocalDate.of(2026, 8, 1))
                .revision(1L).active(true).build();
        UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000204");
        NotificationRecordResponse failed = new NotificationRecordResponse(
                recordId, OWNER_ID, "REMINDER", "Cho con bu", "body", SCHEDULE_ID,
                "REMINDER_SCHEDULE", "FAILED", null, null, false, null,
                "PUSH", null, 1, null, NOW, java.util.Map.of());
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(schedule));
        when(notificationService.sendReminderScheduleNotification(
                eq(SCHEDULE_ID), eq(JOB_ID), eq(OWNER_ID), eq("Cho con bu"),
                any(), any(), any(), any())).thenReturn(failed);
        when(jobRepository.transitionAfterProcessing(
                any(),eq(NotificationJobType.REMINDER_SCHEDULE), any(), eq(AppointmentNotificationJobStatus.PROCESSING), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("transition failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.process(JOB_ID, "worker-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("transition failed");
        verify(jobRepository, times(1)).transitionAfterProcessing(
                any(), any(), any(), eq(AppointmentNotificationJobStatus.PROCESSING),
                any(), any(), any(), any(), any());
    }

    @Test
    void staleBacklogIsSuppressedWithoutCallingTheSender() {
        NotificationJob job = job("worker-1");
        job.setDueAt(NOW.minusSeconds(2 * 60 * 60));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        service.process(JOB_ID, "worker-1");

        org.assertj.core.api.Assertions.assertThat(job.getStatus())
                .isEqualTo(AppointmentNotificationJobStatus.SUPPRESSED);
        org.assertj.core.api.Assertions.assertThat(job.getLastErrorCode())
                .isEqualTo("STALE_BACKLOG");
        verify(notificationService, never()).sendReminderScheduleNotification(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    private static NotificationJob job(String worker) {
        return NotificationJob.builder()
                .jobType(NotificationJobType.REMINDER_SCHEDULE)
                .id(JOB_ID).scheduleId(SCHEDULE_ID).scheduleRevision(1L)
                .occurrenceDate(LocalDate.of(2026, 8, 2)).localTime(LocalTime.of(7, 0))
                .timeZone("UTC").dueAt(NOW).nextAttemptAt(NOW)
                .status(AppointmentNotificationJobStatus.PROCESSING)
                .attemptCount(1).lockedBy(worker).lockedAt(NOW).build();
    }
}
