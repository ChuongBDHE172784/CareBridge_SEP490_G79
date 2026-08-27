package com.carebridge.backend.reminder.schedule.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.schedule.entity.ReminderSchedule;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
public class ReminderScheduleProcessingService {
    /** Every query below is scoped to this type; the appointment worker shares the table. */
    private static final NotificationJobType JOB_TYPE = NotificationJobType.REMINDER_SCHEDULE;

    private final NotificationJobRepository jobRepository;
    private final ReminderScheduleRepository scheduleRepository;
    private final IReminderNotificationService notificationService;
    private final Clock clock;
    private final int maxAttempts;
    private final long staleProcessingMinutes;
    private final long staleBacklogGraceMinutes;

    @Autowired
    public ReminderScheduleProcessingService(
            NotificationJobRepository jobRepository,
            ReminderScheduleRepository scheduleRepository,
            IReminderNotificationService notificationService,
            @Value("${carebridge.notification.reminder-schedule.max-attempts:4}") int maxAttempts,
            @Value("${carebridge.notification.reminder-schedule.stale-processing-minutes:10}")
            long staleProcessingMinutes,
            @Value("${carebridge.notification.reminder-schedule.stale-backlog-grace-minutes:60}")
            long staleBacklogGraceMinutes) {
        this(jobRepository, scheduleRepository, notificationService, Clock.systemUTC(),
                maxAttempts, staleProcessingMinutes, staleBacklogGraceMinutes);
    }

    ReminderScheduleProcessingService(
            NotificationJobRepository jobRepository,
            ReminderScheduleRepository scheduleRepository,
            IReminderNotificationService notificationService,
            Clock clock, int maxAttempts, long staleProcessingMinutes) {
        this(jobRepository, scheduleRepository, notificationService, clock, maxAttempts,
                staleProcessingMinutes, 60);
    }

    ReminderScheduleProcessingService(
            NotificationJobRepository jobRepository,
            ReminderScheduleRepository scheduleRepository,
            IReminderNotificationService notificationService,
            Clock clock, int maxAttempts, long staleProcessingMinutes,
            long staleBacklogGraceMinutes) {
        this.jobRepository = jobRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
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
                AppointmentNotificationJobStatus.PENDING,
                now, PageRequest.of(0, Math.max(1, batchSize)))) {
            if (jobRepository.claim(id, JOB_TYPE, workerId, now,
                    AppointmentNotificationJobStatus.PENDING,
                    AppointmentNotificationJobStatus.PROCESSING) == 1) {
                claimed.add(id);
            }
        }
        return claimed;
    }

    /**
     * The worker identity is a fencing token. A stale async invocation must
     * not send after its PROCESSING row has been requeued and claimed by a
     * newer worker.
     */
    @Async
    @Transactional
    public void processAsync(UUID jobId, String workerId) {
        process(jobId, workerId);
    }

    /** Compatibility entry point for focused/unit callers that do not claim a job. */
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
        ReminderSchedule schedule = scheduleRepository.findById(job.getScheduleId()).orElse(null);
        if (!eligible(job, schedule)) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED,
                    "REMINDER_SCHEDULE_NOT_ACTIVE", null, workerId);
            return;
        }
        String time = job.getLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String body = schedule.getTitle() + " lúc " + time;
        NotificationRecordResponse response;
        try {
            response = notificationService.sendReminderScheduleNotification(
                    schedule.getId(), job.getId(), schedule.getOwnerUserId(), schedule.getTitle(), body,
                    job.getOccurrenceDate(), job.getLocalTime(), job.getTimeZone());
        } catch (RuntimeException exception) {
            retryOrFail(job, "REMINDER_DELIVERY_ERROR", null, workerId);
            return;
        }
        // Keep terminal transition failures outside the delivery catch block. If
        // the database rejects a fenced transition, retrying inside the same
        // aborted transaction only produces a second misleading SQL error.
        if (response == null) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED,
                    "REMINDER_PUSH_DISABLED", null, workerId);
        } else if ("SENT".equals(response.status()) || "DELIVERED".equals(response.status())) {
            finish(job, AppointmentNotificationJobStatus.SENT, null, response.id(), workerId);
        } else {
            retryOrFail(job, "REMINDER_DELIVERY_FAILED", response.id(), workerId);
        }
    }

    private boolean isStaleBacklog(NotificationJob job, Instant now) {
        return job.getDueAt() != null
                && job.getDueAt().isBefore(now.minus(Duration.ofMinutes(staleBacklogGraceMinutes)));
    }

    private boolean eligible(NotificationJob job, ReminderSchedule schedule) {
        return schedule != null && schedule.isActive()
                && job.getScheduleRevision() != null
                && schedule.getRevision() == job.getScheduleRevision().longValue();
    }

    private void retryOrFail(NotificationJob job, String code, UUID recordId, String workerId) {
        if (job.getAttemptCount() >= maxAttempts) {
            finish(job, AppointmentNotificationJobStatus.FAILED, code, recordId, workerId);
            return;
        }
        long delay = Math.min(30L * (1L << Math.min(Math.max(job.getAttemptCount() - 1, 0), 5)), 900L);
        transition(job, AppointmentNotificationJobStatus.PENDING, code, recordId,
                clock.instant().plusSeconds(delay), workerId);
    }

    private void finish(NotificationJob job, AppointmentNotificationJobStatus status,
                        String code, UUID recordId, String workerId) {
        transition(job, status, code, recordId, job.getNextAttemptAt(), workerId);
    }

    private void transition(
            NotificationJob job,
            AppointmentNotificationJobStatus status,
            String code,
            UUID recordId,
            Instant nextAttemptAt,
            String workerId) {
        Instant updatedAt = clock.instant();
        if (workerId == null) {
            applyTransition(job, status, code, recordId, nextAttemptAt, updatedAt);
            jobRepository.save(job);
            return;
        }
        int updated = jobRepository.transitionAfterProcessing(
                job.getId(), JOB_TYPE, workerId, AppointmentNotificationJobStatus.PROCESSING,
                status, nextAttemptAt, code, recordId, updatedAt);
        if (updated == 1) {
            applyTransition(job, status, code, recordId, nextAttemptAt, updatedAt);
        }
    }

    private void applyTransition(
            NotificationJob job,
            AppointmentNotificationJobStatus status,
            String code,
            UUID recordId,
            Instant nextAttemptAt,
            Instant updatedAt) {
        job.setStatus(status);
        job.setLastErrorCode(code);
        job.setNotificationRecordId(recordId);
        job.setNextAttemptAt(nextAttemptAt);
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setUpdatedAt(updatedAt);
    }
}
