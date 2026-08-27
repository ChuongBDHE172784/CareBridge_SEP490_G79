package com.carebridge.backend.reminder.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.ReminderNotificationCommand;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationConfig;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationConfigRepository;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
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

    /** Every query below is scoped to this type; the reminder worker shares the table. */
    private static final NotificationJobType JOB_TYPE = NotificationJobType.APPOINTMENT;

    private final NotificationJobRepository jobRepository;
    private final AppointmentNotificationConfigRepository configRepository;
    private final ReminderRepository reminderRepository;
    private final IReminderNotificationService notificationService;
    private final CareGroupAppointmentNotificationService careGroupNotificationService;
    private final Clock clock;
    private final int maxAttempts;
    private final long staleProcessingMinutes;
    private final long staleBacklogGraceMinutes;

    @Autowired
    public AppointmentNotificationProcessingService(
            NotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            CareGroupAppointmentNotificationService careGroupNotificationService,
            @Value("${carebridge.notification.appointment.max-attempts:4}") int maxAttempts,
            @Value("${carebridge.notification.appointment.stale-processing-minutes:10}") long staleProcessingMinutes,
            @Value("${carebridge.notification.appointment.stale-backlog-grace-minutes:60}")
            long staleBacklogGraceMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService,
                careGroupNotificationService,
                Clock.systemUTC(), maxAttempts, staleProcessingMinutes, staleBacklogGraceMinutes);
    }

    /** Compatibility constructor for worker tests that do not exercise FAMILY fan-out. */
    public AppointmentNotificationProcessingService(
            NotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            @Value("${carebridge.notification.appointment.max-attempts:4}") int maxAttempts,
            @Value("${carebridge.notification.appointment.stale-processing-minutes:10}") long staleProcessingMinutes,
            @Value("${carebridge.notification.appointment.stale-backlog-grace-minutes:60}")
            long staleBacklogGraceMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService, null,
                Clock.systemUTC(), maxAttempts, staleProcessingMinutes, staleBacklogGraceMinutes);
    }

    AppointmentNotificationProcessingService(
            NotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            CareGroupAppointmentNotificationService careGroupNotificationService,
            Clock clock,
            int maxAttempts,
            long staleProcessingMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService,
                careGroupNotificationService, clock,
                maxAttempts, staleProcessingMinutes, 60);
    }

    AppointmentNotificationProcessingService(
            NotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            Clock clock,
            int maxAttempts,
            long staleProcessingMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService, null,
                clock, maxAttempts, staleProcessingMinutes, 60);
    }

    AppointmentNotificationProcessingService(
            NotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            CareGroupAppointmentNotificationService careGroupNotificationService,
            Clock clock,
            int maxAttempts,
            long staleProcessingMinutes,
            long staleBacklogGraceMinutes) {
        this.jobRepository = jobRepository;
        this.configRepository = configRepository;
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.careGroupNotificationService = careGroupNotificationService;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.staleProcessingMinutes = Math.max(0, staleProcessingMinutes);
        this.staleBacklogGraceMinutes = Math.max(0, staleBacklogGraceMinutes);
    }

    @Transactional
    public List<UUID> claimDueJobs(String workerId, int batchSize) {
        Instant now = clock.instant();
        jobRepository.requeueStale(JOB_TYPE,
                now.minus(Duration.ofMinutes(staleProcessingMinutes)), now,
                AppointmentNotificationJobStatus.PENDING,
                AppointmentNotificationJobStatus.PROCESSING);
        List<UUID> claimed = new ArrayList<>();
        for (UUID id : jobRepository.findClaimableIds(JOB_TYPE,
                AppointmentNotificationJobStatus.PENDING, now, PageRequest.of(0, Math.max(1, batchSize)))) {
            if (jobRepository.claim(
                    id, JOB_TYPE, workerId, now,
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
        NotificationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getJobType() != JOB_TYPE
                || job.getStatus() != AppointmentNotificationJobStatus.PROCESSING
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
        NotificationRecordResponse response;
        try {
            response = notificationService.sendAppointmentNotification(
                    new ReminderNotificationCommand(
                            job.getId(), reminder.getId(), job.getOccurrenceId(), reminder.getOwnerUserId(),
                            reminder.getTitle(), job.getOccurrenceScheduledAt(), job.getOffsetMinutes(),
                            config.getTimeZone()));
        } catch (RuntimeException exception) {
            retryOrFail(job, "APPOINTMENT_DELIVERY_ERROR", null, workerId);
            return;
        }
        if (careGroupNotificationService != null) {
            try {
                careGroupNotificationService.notifyMilestone(reminder, job, config.getTimeZone());
            } catch (RuntimeException ignored) {
                // FAMILY fan-out must not retry the mother's durable milestone job.
            }
        }
        // Keep terminal transition failures outside the delivery catch block. If
        // the database rejects a fenced transition, retrying inside the same
        // aborted transaction only produces a second misleading SQL error.
        if (response == null) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "REMINDER_PUSH_DISABLED", null, workerId);
        } else if ("SENT".equals(response.status()) || "DELIVERED".equals(response.status())) {
            finish(job, AppointmentNotificationJobStatus.SENT, null, response.id(), workerId);
        } else {
            retryOrFail(job, "APPOINTMENT_DELIVERY_FAILED", response.id(), workerId);
        }
    }

    private boolean isStaleBacklog(NotificationJob job, Instant now) {
        return job.getDueAt() != null
                && job.getDueAt().isBefore(now.minus(Duration.ofMinutes(staleBacklogGraceMinutes)));
    }

    private boolean eligible(
            NotificationJob job,
            Reminder reminder,
            AppointmentNotificationConfig config) {
        if (reminder == null || config == null) return false;
        if (job.getConfigRevision() == null
                || config.getConfigRevision() != job.getConfigRevision().longValue()) return false;
        if (job.getOccurrenceGeneration() == null
                || reminder.getOccurrenceGeneration() != job.getOccurrenceGeneration().longValue()) return false;
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
            NotificationJob job, String errorCode, UUID recordId, String workerId) {
        if (job.getAttemptCount() >= maxAttempts) {
            finish(job, AppointmentNotificationJobStatus.FAILED, errorCode, recordId, workerId);
            return;
        }
        long delaySeconds = Math.min(30L * (1L << Math.min(Math.max(job.getAttemptCount() - 1, 0), 5)), 900L);
        transition(job, AppointmentNotificationJobStatus.PENDING, errorCode, recordId,
                clock.instant().plusSeconds(delaySeconds), workerId);
    }

    private void finish(
            NotificationJob job,
            AppointmentNotificationJobStatus status,
            String errorCode,
            UUID notificationRecordId,
            String workerId) {
        transition(job, status, errorCode, notificationRecordId, job.getNextAttemptAt(), workerId);
    }

    private void transition(
            NotificationJob job,
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
                job.getId(), JOB_TYPE, workerId, AppointmentNotificationJobStatus.PROCESSING,
                status, nextAttemptAt, errorCode, notificationRecordId, updatedAt);
        if (updated == 1) {
            applyTransition(job, status, errorCode, notificationRecordId, nextAttemptAt, updatedAt);
        }
    }

    private void applyTransition(
            NotificationJob job,
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
