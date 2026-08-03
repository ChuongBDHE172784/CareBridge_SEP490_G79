package com.carebridge.backend.reminder.schedule.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.schedule.entity.ReminderSchedule;
import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleJob;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleJobRepository;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
    private final ReminderScheduleJobRepository jobRepository;
    private final ReminderScheduleRepository scheduleRepository;
    private final IReminderNotificationService notificationService;
    private final Clock clock;
    private final int maxAttempts;
    private final long staleProcessingMinutes;

    @Autowired
    public ReminderScheduleProcessingService(
            ReminderScheduleJobRepository jobRepository,
            ReminderScheduleRepository scheduleRepository,
            IReminderNotificationService notificationService,
            @Value("${carebridge.notification.reminder-schedule.max-attempts:4}") int maxAttempts,
            @Value("${carebridge.notification.reminder-schedule.stale-processing-minutes:10}")
            long staleProcessingMinutes) {
        this(jobRepository, scheduleRepository, notificationService, Clock.systemUTC(),
                maxAttempts, staleProcessingMinutes);
    }

    ReminderScheduleProcessingService(
            ReminderScheduleJobRepository jobRepository,
            ReminderScheduleRepository scheduleRepository,
            IReminderNotificationService notificationService,
            Clock clock, int maxAttempts, long staleProcessingMinutes) {
        this.jobRepository = jobRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.staleProcessingMinutes = staleProcessingMinutes;
    }

    @Transactional
    public List<UUID> claimDueJobs(String workerId, int batchSize) {
        Instant now = clock.instant();
        jobRepository.requeueStale(now.minus(Duration.ofMinutes(staleProcessingMinutes)), now,
                AppointmentNotificationJobStatus.PENDING,
                AppointmentNotificationJobStatus.PROCESSING);
        List<UUID> claimed = new ArrayList<>();
        for (UUID id : jobRepository.findClaimableIds(AppointmentNotificationJobStatus.PENDING,
                now, PageRequest.of(0, batchSize))) {
            if (jobRepository.claim(id, workerId, now, AppointmentNotificationJobStatus.PENDING,
                    AppointmentNotificationJobStatus.PROCESSING) == 1) {
                claimed.add(id);
            }
        }
        return claimed;
    }

    /**
     * The worker identity is a fencing token.  A stale async invocation must
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
        ReminderScheduleJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AppointmentNotificationJobStatus.PROCESSING
                || (workerId != null && !workerId.equals(job.getLockedBy()))) return;
        ReminderSchedule schedule = scheduleRepository.findById(job.getScheduleId()).orElse(null);
        if (!eligible(job, schedule)) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "REMINDER_SCHEDULE_NOT_ACTIVE", null);
            return;
        }
        String time = job.getLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String body = schedule.getTitle() + " lúc " + time;
        try {
            NotificationRecordResponse response = notificationService.sendReminderScheduleNotification(
                    schedule.getId(), job.getId(), schedule.getOwnerUserId(), schedule.getTitle(), body,
                    job.getOccurrenceDate(), job.getLocalTime(), job.getTimeZone());
            if (response == null) {
                finish(job, AppointmentNotificationJobStatus.SUPPRESSED,
                        "REMINDER_PUSH_DISABLED", null);
            } else if ("SENT".equals(response.status()) || "DELIVERED".equals(response.status())) {
                finish(job, AppointmentNotificationJobStatus.SENT, null, response.id());
            } else {
                retryOrFail(job, "REMINDER_DELIVERY_FAILED", response.id());
            }
        } catch (RuntimeException exception) {
            retryOrFail(job, "REMINDER_DELIVERY_ERROR", null);
        }
    }

    private boolean eligible(ReminderScheduleJob job, ReminderSchedule schedule) {
        return schedule != null && schedule.isActive()
                && schedule.getRevision() == job.getScheduleRevision();
    }

    private void retryOrFail(ReminderScheduleJob job, String code, UUID recordId) {
        if (job.getAttemptCount() >= maxAttempts) {
            finish(job, AppointmentNotificationJobStatus.FAILED, code, recordId);
            return;
        }
        long delay = Math.min(30L * (1L << Math.min(Math.max(job.getAttemptCount() - 1, 0), 5)), 900L);
        job.setStatus(AppointmentNotificationJobStatus.PENDING);
        job.setNextAttemptAt(clock.instant().plusSeconds(delay));
        job.setLastErrorCode(code);
        job.setNotificationRecordId(recordId);
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setUpdatedAt(clock.instant());
        jobRepository.save(job);
    }

    private void finish(ReminderScheduleJob job, AppointmentNotificationJobStatus status,
                        String code, UUID recordId) {
        job.setStatus(status);
        job.setLastErrorCode(code);
        job.setNotificationRecordId(recordId);
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setUpdatedAt(clock.instant());
        jobRepository.save(job);
    }
}
