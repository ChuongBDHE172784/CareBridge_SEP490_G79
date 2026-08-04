package com.carebridge.backend.reminder.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.ReminderNotificationCommand;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationConfig;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJob;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationConfigRepository;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationJobRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentNotificationProcessingService {

    private final AppointmentNotificationJobRepository jobRepository;
    private final AppointmentNotificationConfigRepository configRepository;
    private final ReminderRepository reminderRepository;
    private final IReminderNotificationService notificationService;
    private final Clock clock;
    private final int maxAttempts;
    private final long staleProcessingMinutes;
    private final long staleBacklogGraceMinutes;

    @Autowired
    public AppointmentNotificationProcessingService(
            AppointmentNotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            @Value("${carebridge.notification.appointment.max-attempts:4}") int maxAttempts,
            @Value("${carebridge.notification.appointment.stale-processing-minutes:10}") long staleProcessingMinutes,
            @Value("${carebridge.notification.appointment.stale-backlog-grace-minutes:60}")
            long staleBacklogGraceMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService,
                Clock.systemUTC(), maxAttempts, staleProcessingMinutes, staleBacklogGraceMinutes);
    }

    AppointmentNotificationProcessingService(
            AppointmentNotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            Clock clock,
            int maxAttempts,
            long staleProcessingMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService, clock,
                maxAttempts, staleProcessingMinutes, 60);
    }

    AppointmentNotificationProcessingService(
            AppointmentNotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            Clock clock,
            int maxAttempts,
            long staleProcessingMinutes,
            long staleBacklogGraceMinutes) {
        this.jobRepository = jobRepository;
        this.configRepository = configRepository;
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.staleProcessingMinutes = Math.max(0, staleProcessingMinutes);
        this.staleBacklogGraceMinutes = Math.max(0, staleBacklogGraceMinutes);
    }

    @Transactional
    public List<UUID> claimDueJobs(String workerId, int batchSize) {
        Instant now = clock.instant();
        jobRepository.requeueStale(
                now.minus(Duration.ofMinutes(staleProcessingMinutes)), now,
                AppointmentNotificationJobStatus.PENDING,
                AppointmentNotificationJobStatus.PROCESSING);
        List<UUID> claimed = new ArrayList<>();
        for (UUID id : jobRepository.findClaimableIds(
                AppointmentNotificationJobStatus.PENDING, now, PageRequest.of(0, Math.max(1, batchSize)))) {
            if (jobRepository.claim(
                    id, workerId, now,
                    AppointmentNotificationJobStatus.PENDING,
                    AppointmentNotificationJobStatus.PROCESSING) == 1) {
                claimed.add(id);
            }
        }
        return claimed;
    }

    @Async
    @Transactional
    public void processAsync(UUID jobId, String workerId) {
        process(jobId, workerId);
    }

    /** Compatibility entry point for callers that process an already-loaded job directly. */
    @Async
    @Transactional
    public void processAsync(UUID jobId) {
        process(jobId, null);
    }

    @Transactional
    public void process(UUID jobId) {
        process(jobId, null);
    }

    @Transactional
    public void process(UUID jobId, String workerId) {
        AppointmentNotificationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AppointmentNotificationJobStatus.PROCESSING
                || (workerId != null && !workerId.equals(job.getLockedBy()))) return;
        Instant now = clock.instant();
        if (isStaleBacklog(job, now)) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "STALE_BACKLOG", null, workerId);
            return;
        }
        Reminder reminder = reminderRepository.findById(job.getReminderId()).orElse(null);
        AppointmentNotificationConfig config = configRepository.findById(job.getReminderId()).orElse(null);
        if (!eligible(job, reminder, config)) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "APPOINTMENT_NOT_ACTIVE", null, workerId);
            return;
        }
        try {
            NotificationRecordResponse response = notificationService.sendAppointmentNotification(
                    new ReminderNotificationCommand(
                            job.getId(), reminder.getId(), job.getOccurrenceId(), reminder.getOwnerUserId(),
                            reminder.getTitle(), job.getOccurrenceScheduledAt(), job.getOffsetMinutes(),
                            config.getTimeZone()));
            if (response == null) {
                finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "REMINDER_PUSH_DISABLED", null, workerId);
            } else if ("SENT".equals(response.status()) || "DELIVERED".equals(response.status())) {
                finish(job, AppointmentNotificationJobStatus.SENT, null, response.id(), workerId);
            } else {
                retryOrFail(job, "APPOINTMENT_DELIVERY_FAILED", response.id(), workerId);
            }
        } catch (RuntimeException exception) {
            retryOrFail(job, "APPOINTMENT_DELIVERY_ERROR", null, workerId);
        }
    }

    private boolean isStaleBacklog(AppointmentNotificationJob job, Instant now) {
        return job.getDueAt() != null
                && job.getDueAt().isBefore(now.minus(Duration.ofMinutes(staleBacklogGraceMinutes)));
    }

    private boolean eligible(
            AppointmentNotificationJob job,
            Reminder reminder,
            AppointmentNotificationConfig config) {
        if (reminder == null || config == null) return false;
        if (config.getConfigRevision() != job.getConfigRevision()) return false;
        if (reminder.getOccurrenceGeneration() != job.getOccurrenceGeneration()) return false;
        ReminderStatus status = reminder.getStatus();
        boolean recurring = reminder.getRecurrenceType() != null
                && reminder.getRecurrenceType() != RecurrenceType.NONE;
        if (status == ReminderStatus.CANCELLED) return false;
        if (!recurring && status != ReminderStatus.PENDING && status != ReminderStatus.SNOOZED) {
            return false;
        }
        if (job.getOffsetMinutes() > 0) {
            return recurring || status == ReminderStatus.PENDING || status == ReminderStatus.SNOOZED;
        }
        return true;
    }

    private void retryOrFail(
            AppointmentNotificationJob job, String errorCode, UUID recordId, String workerId) {
        if (job.getAttemptCount() >= maxAttempts) {
            finish(job, AppointmentNotificationJobStatus.FAILED, errorCode, recordId, workerId);
            return;
        }
        long delaySeconds = Math.min(30L * (1L << Math.min(Math.max(job.getAttemptCount() - 1, 0), 5)), 900L);
        transition(job, AppointmentNotificationJobStatus.PENDING, errorCode, recordId,
                clock.instant().plusSeconds(delaySeconds), workerId);
    }

    private void finish(
            AppointmentNotificationJob job,
            AppointmentNotificationJobStatus status,
            String errorCode,
            UUID notificationRecordId,
            String workerId) {
        transition(job, status, errorCode, notificationRecordId, job.getNextAttemptAt(), workerId);
    }

    private void transition(
            AppointmentNotificationJob job,
            AppointmentNotificationJobStatus status,
            String errorCode,
            UUID notificationRecordId,
            Instant nextAttemptAt,
            String workerId) {
        Instant updatedAt = clock.instant();
        if (workerId == null) {
            applyTransition(job, status, errorCode, notificationRecordId, nextAttemptAt, updatedAt);
            jobRepository.save(job);
            return;
        }
        int updated = jobRepository.transitionAfterProcessing(
                job.getId(), workerId, AppointmentNotificationJobStatus.PROCESSING,
                status, nextAttemptAt, errorCode, notificationRecordId, updatedAt);
        if (updated == 1) {
            applyTransition(job, status, errorCode, notificationRecordId, nextAttemptAt, updatedAt);
        }
    }

    private void applyTransition(
            AppointmentNotificationJob job,
            AppointmentNotificationJobStatus status,
            String errorCode,
            UUID notificationRecordId,
            Instant nextAttemptAt,
            Instant updatedAt) {
        job.setStatus(status);
        job.setLastErrorCode(errorCode);
        job.setNotificationRecordId(notificationRecordId);
        job.setNextAttemptAt(nextAttemptAt);
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setUpdatedAt(updatedAt);
    }
}
