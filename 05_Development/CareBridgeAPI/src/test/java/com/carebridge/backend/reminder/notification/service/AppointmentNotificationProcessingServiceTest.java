package com.carebridge.backend.reminder.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationConfig;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJob;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationConfigRepository;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationJobRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AppointmentNotificationProcessingServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T00:00:00Z");
    private static final UUID JOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID REMINDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000204");
    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000205");

    @Mock private AppointmentNotificationJobRepository jobRepository;
    @Mock private AppointmentNotificationConfigRepository configRepository;
    @Mock private ReminderRepository reminderRepository;
    @Mock private IReminderNotificationService notificationService;

    private AppointmentNotificationProcessingService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentNotificationProcessingService(
                jobRepository,
                configRepository,
                reminderRepository,
                notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC),
                4,
                10);
        org.mockito.Mockito.lenient().when(jobRepository.transitionAfterProcessing(
                any(), any(), eq(AppointmentNotificationJobStatus.PROCESSING), any(), any(), any(), any(), any()))
                .thenReturn(1);
    }

    @Test
    void claimDueJobs_recoversStaleLeasesAndReturnsOnlyAtomicClaims() {
        UUID otherId = UUID.fromString("00000000-0000-0000-0000-000000000206");
        when(jobRepository.findClaimableIds(
                eq(AppointmentNotificationJobStatus.PENDING), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of(JOB_ID, otherId));
        when(jobRepository.claim(
                eq(JOB_ID), eq("worker-a"), eq(NOW),
                eq(AppointmentNotificationJobStatus.PENDING),
                eq(AppointmentNotificationJobStatus.PROCESSING)))
                .thenReturn(1);
        when(jobRepository.claim(
                eq(otherId), eq("worker-a"), eq(NOW),
                eq(AppointmentNotificationJobStatus.PENDING),
                eq(AppointmentNotificationJobStatus.PROCESSING)))
                .thenReturn(0);

        List<UUID> claimed = service.claimDueJobs("worker-a", 10);

        assertThat(claimed).containsExactly(JOB_ID);
        verify(jobRepository).requeueStale(
                eq(NOW.minusSeconds(10 * 60)),
                eq(NOW),
                eq(AppointmentNotificationJobStatus.PENDING),
                eq(AppointmentNotificationJobStatus.PROCESSING));
    }

    @Test
    void claimDueJobs_clampsNonPositiveBatchSize() {
        when(jobRepository.findClaimableIds(
                eq(AppointmentNotificationJobStatus.PENDING), eq(NOW), any(Pageable.class)))
                .thenReturn(List.of());

        service.claimDueJobs("worker-a", 0);

        org.mockito.ArgumentCaptor<Pageable> pageable =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(jobRepository).findClaimableIds(
                eq(AppointmentNotificationJobStatus.PENDING), eq(NOW), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void process_successMarksJobSent() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 1L, 0L);
        when(notificationService.sendAppointmentNotification(any()))
                .thenReturn(response("SENT"));

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.SENT);
        assertThat(job.getNotificationRecordId()).isEqualTo(RECORD_ID);
        assertThat(job.getLockedBy()).isNull();
        assertThat(job.getLockedAt()).isNull();
    }

    @Test
    void process_deliveredRecordAlsoCompletesJobWithoutRetry() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 1L, 0L);
        when(notificationService.sendAppointmentNotification(any()))
                .thenReturn(response("DELIVERED"));

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.SENT);
        assertThat(job.getLastErrorCode()).isNull();
    }

    @Test
    void process_revisionMismatchSuppressesWithoutDelivery() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 2L, 0L);

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.SUPPRESSED);
        assertThat(job.getLastErrorCode()).isEqualTo("APPOINTMENT_NOT_ACTIVE");
        verify(notificationService, never()).sendAppointmentNotification(any());
    }

    @Test
    void process_generationMismatchSuppressesWithoutDelivery() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 1L, 1L);

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.SUPPRESSED);
        verify(notificationService, never()).sendAppointmentNotification(any());
    }

    @Test
    void process_deliveryFailureRequeuesWithBoundedBackoff() {
        AppointmentNotificationJob job = processingJob(2, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 1L, 0L);
        when(notificationService.sendAppointmentNotification(any()))
                .thenThrow(new IllegalStateException("temporary FCM failure"));

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.PENDING);
        assertThat(job.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(job.getLastErrorCode()).isEqualTo("APPOINTMENT_DELIVERY_ERROR");
        assertThat(job.getLockedBy()).isNull();
        assertThat(job.getLockedAt()).isNull();
    }

    @Test
    void process_deliveryFailureAtMaxAttemptsMarksFailed() {
        AppointmentNotificationJob job = processingJob(4, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 1L, 0L);
        when(notificationService.sendAppointmentNotification(any()))
                .thenThrow(new IllegalStateException("permanent FCM failure"));

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.FAILED);
        assertThat(job.getLastErrorCode()).isEqualTo("APPOINTMENT_DELIVERY_ERROR");
    }

    @Test
    void process_terminalTransitionFailureIsNotRetriedInsideTheAbortedTransaction() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        stubEligible(job, ReminderStatus.PENDING, RecurrenceType.NONE, 1L, 0L);
        when(notificationService.sendAppointmentNotification(any())).thenReturn(response("FAILED"));
        when(jobRepository.transitionAfterProcessing(
                any(), any(), eq(AppointmentNotificationJobStatus.PROCESSING), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("transition failed"));

        assertThatThrownBy(() -> service.process(JOB_ID, "worker-a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("transition failed");
        verify(jobRepository, times(1)).transitionAfterProcessing(
                any(), any(), eq(AppointmentNotificationJobStatus.PROCESSING), any(), any(), any(), any(), any());
    }

    @Test
    void process_positiveOffsetForCompletedOneTimeAppointmentIsSuppressed() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, 15);
        stubEligible(job, ReminderStatus.COMPLETED, RecurrenceType.NONE, 1L, 0L);

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.SUPPRESSED);
        verify(notificationService, never()).sendAppointmentNotification(any());
    }

    @Test
    void process_staleBacklogSuppressesWithoutDelivery() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        job.setDueAt(NOW.minusSeconds(2 * 60 * 60));
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        service.process(JOB_ID, "worker-a");

        assertThat(job.getStatus()).isEqualTo(AppointmentNotificationJobStatus.SUPPRESSED);
        assertThat(job.getLastErrorCode()).isEqualTo("STALE_BACKLOG");
        verify(notificationService, never()).sendAppointmentNotification(any());
    }

    @Test
    void process_rejectsStaleWorkerBeforeDelivery() {
        AppointmentNotificationJob job = processingJob(1, 1L, 0L, -30);
        job.setLockedBy("new-worker");
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

        service.process(JOB_ID, "old-worker");

        verify(notificationService, never()).sendAppointmentNotification(any());
        verify(jobRepository, never()).save(job);
    }

    private void stubEligible(
            AppointmentNotificationJob job,
            ReminderStatus status,
            RecurrenceType recurrence,
            long configRevision,
            long occurrenceGeneration) {
        Reminder reminder = Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(USER_ID)
                .reminderType(ReminderType.APPOINTMENT)
                .title("Prenatal appointment")
                .scheduledAt(job.getOccurrenceScheduledAt())
                .recurrenceType(recurrence)
                .status(status)
                .occurrenceGeneration(occurrenceGeneration)
                .build();
        AppointmentNotificationConfig config = AppointmentNotificationConfig.builder()
                .reminderId(REMINDER_ID)
                .timeZone("Asia/Ho_Chi_Minh")
                .configRevision(configRevision)
                .createdAt(NOW.minusSeconds(60))
                .updatedAt(NOW.minusSeconds(60))
                .build();
        when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(configRepository.findById(REMINDER_ID)).thenReturn(Optional.of(config));
    }

    private AppointmentNotificationJob processingJob(
            int attempts,
            long configRevision,
            long occurrenceGeneration,
            int offsetMinutes) {
        return AppointmentNotificationJob.builder()
                .id(JOB_ID)
                .reminderId(REMINDER_ID)
                .occurrenceId(OCCURRENCE_ID)
                .occurrenceGeneration(occurrenceGeneration)
                .occurrenceScheduledAt(NOW.plusSeconds(60 * 60))
                .configRevision(configRevision)
                .offsetMinutes(offsetMinutes)
                .dueAt(NOW)
                .status(AppointmentNotificationJobStatus.PROCESSING)
                .attemptCount(attempts)
                .nextAttemptAt(NOW)
                .lockedBy("worker-a")
                .lockedAt(NOW)
                .createdAt(NOW.minusSeconds(60))
                .updatedAt(NOW)
                .build();
    }

    private NotificationRecordResponse response(String status) {
        return new NotificationRecordResponse(
                RECORD_ID,
                USER_ID,
                "REMINDER",
                "Appointment reminder",
                "Appointment body",
                REMINDER_ID,
                "APPOINTMENT",
                status,
                NOW,
                NOW,
                false,
                null,
                "PUSH",
                "fcm-message-id",
                1,
                null,
                NOW,
                Map.of("milestoneJobId", JOB_ID.toString()));
    }
}
