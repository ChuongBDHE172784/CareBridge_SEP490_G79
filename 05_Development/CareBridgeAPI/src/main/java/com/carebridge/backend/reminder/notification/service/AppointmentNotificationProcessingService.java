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

    @Autowired
    public AppointmentNotificationProcessingService(
            AppointmentNotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            @Value("${carebridge.notification.appointment.max-attempts:4}") int maxAttempts,
            @Value("${carebridge.notification.appointment.stale-processing-minutes:10}") long staleProcessingMinutes) {
        this(jobRepository, configRepository, reminderRepository, notificationService,
                Clock.systemUTC(), maxAttempts, staleProcessingMinutes);
    }

    AppointmentNotificationProcessingService(
            AppointmentNotificationJobRepository jobRepository,
            AppointmentNotificationConfigRepository configRepository,
            ReminderRepository reminderRepository,
            IReminderNotificationService notificationService,
            Clock clock,
            int maxAttempts,
            long staleProcessingMinutes) {
        this.jobRepository = jobRepository;
        this.configRepository = configRepository;
        this.reminderRepository = reminderRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.staleProcessingMinutes = staleProcessingMinutes;
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
                AppointmentNotificationJobStatus.PENDING, now, PageRequest.of(0, batchSize))) {
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
    public void processAsync(UUID jobId) {
        process(jobId);
    }

    @Transactional
    public void process(UUID jobId) {
        AppointmentNotificationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != AppointmentNotificationJobStatus.PROCESSING) return;
        Reminder reminder = reminderRepository.findById(job.getReminderId()).orElse(null);
        AppointmentNotificationConfig config = configRepository.findById(job.getReminderId()).orElse(null);
        if (!eligible(job, reminder, config)) {
            finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "APPOINTMENT_NOT_ACTIVE", null);
            return;
        }
        try {
            NotificationRecordResponse response = notificationService.sendAppointmentNotification(
                    new ReminderNotificationCommand(
                            job.getId(), reminder.getId(), job.getOccurrenceId(), reminder.getOwnerUserId(),
                            reminder.getTitle(), job.getOccurrenceScheduledAt(), job.getOffsetMinutes(),
                            config.getTimeZone()));
            if (response == null) {
                finish(job, AppointmentNotificationJobStatus.SUPPRESSED, "REMINDER_PUSH_DISABLED", null);
            } else if ("SENT".equals(response.status())) {
                finish(job, AppointmentNotificationJobStatus.SENT, null, response.id());
            } else {
                retryOrFail(job, "APPOINTMENT_DELIVERY_FAILED", response.id());
            }
        } catch (RuntimeException exception) {
            retryOrFail(job, "APPOINTMENT_DELIVERY_ERROR", null);
        }
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

    private void retryOrFail(AppointmentNotificationJob job, String errorCode, UUID recordId) {
        if (job.getAttemptCount() >= maxAttempts) {
            finish(job, AppointmentNotificationJobStatus.FAILED, errorCode, recordId);
            return;
        }
        long delaySeconds = Math.min(30L * (1L << Math.min(Math.max(job.getAttemptCount() - 1, 0), 5)), 900L);
        job.setStatus(AppointmentNotificationJobStatus.PENDING);
        job.setNextAttemptAt(clock.instant().plusSeconds(delaySeconds));
        job.setLastErrorCode(errorCode);
        job.setNotificationRecordId(recordId);
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setUpdatedAt(clock.instant());
        jobRepository.save(job);
    }

    private void finish(
            AppointmentNotificationJob job,
            AppointmentNotificationJobStatus status,
            String errorCode,
            UUID notificationRecordId) {
        job.setStatus(status);
        job.setLastErrorCode(errorCode);
        job.setNotificationRecordId(notificationRecordId);
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setUpdatedAt(clock.instant());
        jobRepository.save(job);
    }
}
