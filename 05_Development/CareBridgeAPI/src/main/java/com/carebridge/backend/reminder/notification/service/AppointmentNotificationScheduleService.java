package com.carebridge.backend.reminder.notification.service;

import com.carebridge.backend.checklist.today.provider.ReminderOccurrenceIdFactory;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationConfig;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.notification.repository.AppointmentNotificationConfigRepository;
import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.job.repository.NotificationJobRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentNotificationScheduleService {

    private static final int HORIZON_DAYS = 35;
    private static final EnumSet<AppointmentNotificationJobStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentNotificationJobStatus.PENDING, AppointmentNotificationJobStatus.PROCESSING);

    private final AppointmentNotificationConfigRepository configRepository;
    private static final NotificationJobType JOB_TYPE = NotificationJobType.APPOINTMENT;

    private final NotificationJobRepository jobRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final AppointmentNotificationRuleValidator validator;
    private final Clock clock;

    @Autowired
    public AppointmentNotificationScheduleService(
            AppointmentNotificationConfigRepository configRepository,
            NotificationJobRepository jobRepository,
            NotificationPreferenceRepository preferenceRepository,
            AppointmentNotificationRuleValidator validator) {
        this(configRepository, jobRepository, preferenceRepository, validator, Clock.systemUTC());
    }

    AppointmentNotificationScheduleService(
            AppointmentNotificationConfigRepository configRepository,
            NotificationJobRepository jobRepository,
            NotificationPreferenceRepository preferenceRepository,
            AppointmentNotificationRuleValidator validator,
            Clock clock) {
        this.configRepository = configRepository;
        this.jobRepository = jobRepository;
        this.preferenceRepository = preferenceRepository;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public List<Integer> createSnapshot(
            Reminder reminder, List<Integer> requestedOffsets, String requestedTimeZone) {
        if (reminder.getReminderType() != ReminderType.APPOINTMENT) return List.of();
        List<Integer> effective = requestedOffsets == null
                ? globalDefaults(reminder.getOwnerUserId())
                : validator.normalize(requestedOffsets);
        String timeZone = validator.normalizeTimeZone(requestedTimeZone);
        Instant now = clock.instant();
        AppointmentNotificationConfig config = AppointmentNotificationConfig.builder()
                .reminderId(reminder.getId())
                .timeZone(timeZone)
                .configRevision(1L)
                .createdAt(now)
                .updatedAt(now)
                .build();
        config.replaceOffsetMinutes(effective);
        configRepository.save(config);
        materialize(reminder, config, effective, now);
        return effective;
    }

    @Transactional
    public List<Integer> reschedule(
            Reminder reminder,
            List<Integer> requestedOffsets,
            boolean replaceOffsets,
            String requestedTimeZone) {
        if (reminder.getReminderType() != ReminderType.APPOINTMENT) return List.of();
        Instant now = clock.instant();
        AppointmentNotificationConfig config = configRepository.findById(reminder.getId())
                .orElseGet(() -> AppointmentNotificationConfig.builder()
                        .reminderId(reminder.getId())
                        .timeZone(AppointmentNotificationRuleValidator.DEFAULT_TIME_ZONE)
                        .configRevision(0L)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
        List<Integer> effective = replaceOffsets
                ? validator.normalize(requestedOffsets == null ? List.of() : requestedOffsets)
                : config.offsetMinutes();
        if (!replaceOffsets && effective.isEmpty() && config.getConfigRevision() == 0L) {
            effective = globalDefaults(reminder.getOwnerUserId());
        }
        config.setConfigRevision(config.getConfigRevision() + 1L);
        config.setTimeZone(requestedTimeZone == null
                ? validator.normalizeTimeZone(config.getTimeZone())
                : validator.normalizeTimeZone(requestedTimeZone));
        config.setUpdatedAt(now);
        // The offsets and the revision they belong to are written in one save, in
        // one transaction: a job materialised below snapshots this exact revision.
        boolean rewriteOffsets = replaceOffsets || config.offsetMinutes().isEmpty();
        if (rewriteOffsets) {
            config.replaceOffsetMinutes(effective);
        }
        configRepository.save(config);
        if (rewriteOffsets) {
        }
        jobRepository.cancelObsoleteConfigRevisions(
                reminder.getId(), config.getConfigRevision(), ACTIVE_STATUSES,
                AppointmentNotificationJobStatus.CANCELLED, now);
        materialize(reminder, config, effective, now);
        return effective;
    }

    @Transactional(readOnly = true)
    public List<Integer> currentOffsets(UUID reminderId) {
        return configRepository.findById(reminderId)
                .map(AppointmentNotificationConfig::offsetMinutes)
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public String currentTimeZone(UUID reminderId) {
        return configRepository.findById(reminderId)
                .map(AppointmentNotificationConfig::getTimeZone)
                .orElse(AppointmentNotificationRuleValidator.DEFAULT_TIME_ZONE);
    }

    @Transactional
    public void cancelRemaining(UUID reminderId) {
        jobRepository.cancelActiveByReminderId(
                reminderId, ACTIVE_STATUSES, AppointmentNotificationJobStatus.CANCELLED, clock.instant());
    }

    @Transactional
    public void cancelOccurrence(UUID reminderId, UUID occurrenceId) {
        jobRepository.cancelActiveByOccurrenceId(
                reminderId, occurrenceId, ACTIVE_STATUSES,
                AppointmentNotificationJobStatus.CANCELLED, clock.instant());
    }

    @Transactional
    public void extendHorizon(Reminder reminder) {
        if (reminder.getReminderType() != ReminderType.APPOINTMENT) return;
        configRepository.findById(reminder.getId()).ifPresent(config ->
                materialize(reminder, config, config.offsetMinutes(), clock.instant()));
    }

    private List<Integer> globalDefaults(UUID userId) {
        List<Integer> stored = preferenceRepository.findAppointmentReminderDefaults(userId);
        return preferenceRepository.hasAppointmentReminderDefaults(userId)
                ? validator.normalize(stored)
                : AppointmentNotificationRuleValidator.SYSTEM_DEFAULTS;
    }

    private void materialize(
            Reminder reminder,
            AppointmentNotificationConfig config,
            List<Integer> offsets,
            Instant now) {
        if (offsets.isEmpty()) return;
        for (Instant occurrence : occurrences(reminder, config.getTimeZone(), now)) {
            UUID occurrenceId = ReminderOccurrenceIdFactory.create(
                    reminder.getId(), occurrence, reminder.getOccurrenceGeneration());
            for (int offset : offsets) {
                Instant dueAt = occurrence.plus(offset, ChronoUnit.MINUTES);
                if (!dueAt.isAfter(now)) continue;
                boolean exists = jobRepository
                        .existsByJobTypeAndReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(
                                JOB_TYPE, reminder.getId(), occurrenceId,
                                config.getConfigRevision(), offset);
                if (exists) continue;
                NotificationJob job = NotificationJob.builder()
                        .jobType(JOB_TYPE)
                        .reminderId(reminder.getId())
                        .occurrenceId(occurrenceId)
                        .occurrenceGeneration(reminder.getOccurrenceGeneration())
                        .occurrenceScheduledAt(occurrence)
                        .configRevision(config.getConfigRevision())
                        .offsetMinutes(offset)
                        .dueAt(dueAt)
                        .status(AppointmentNotificationJobStatus.PENDING)
                        .attemptCount(0)
                        .nextAttemptAt(dueAt)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                jobRepository.save(job);
            }
        }
    }

    private List<Instant> occurrences(Reminder reminder, String timeZone, Instant now) {
        RecurrenceType recurrence = reminder.getRecurrenceType() == null
                ? RecurrenceType.NONE : reminder.getRecurrenceType();
        if (recurrence == RecurrenceType.NONE) return List.of(reminder.getScheduledAt());
        ZoneId zone = ZoneId.of(timeZone);
        LocalDateTime base = LocalDateTime.ofInstant(reminder.getScheduledAt(), zone);
        LocalDate start = LocalDate.now(clock.withZone(zone));
        LocalDate end = start.plusDays(HORIZON_DAYS);
        if (reminder.getRecurrenceEndDate() != null) {
            LocalDate configuredEnd = LocalDateTime.ofInstant(reminder.getRecurrenceEndDate(), zone).toLocalDate();
            if (configuredEnd.isBefore(end)) end = configuredEnd;
        }
        List<Instant> values = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (date.isBefore(base.toLocalDate())) continue;
            boolean occurs = switch (recurrence) {
                case DAILY -> true;
                case WEEKLY -> date.getDayOfWeek() == base.getDayOfWeek();
                case MONTHLY -> date.getDayOfMonth() == base.getDayOfMonth()
                        && YearMonth.from(date).lengthOfMonth() >= base.getDayOfMonth();
                case NONE -> false;
            };
            if (occurs) values.add(date.atTime(base.toLocalTime()).atZone(zone).toInstant());
        }
        return values;
    }
}
