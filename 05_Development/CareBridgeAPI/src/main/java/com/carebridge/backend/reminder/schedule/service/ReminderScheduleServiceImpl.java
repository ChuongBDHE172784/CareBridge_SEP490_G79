package com.carebridge.backend.reminder.schedule.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.schedule.dto.CreateReminderScheduleRequest;
import com.carebridge.backend.reminder.schedule.dto.ReminderScheduleResponse;
import com.carebridge.backend.reminder.schedule.dto.UpdateReminderScheduleRequest;
import com.carebridge.backend.reminder.schedule.entity.ReminderSchedule;
import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleJob;
import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleRecurrence;
import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleTime;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleJobRepository;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleRepository;
import com.carebridge.backend.reminder.schedule.repository.ReminderScheduleTimeRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderScheduleServiceImpl implements ReminderScheduleService {
    private static final int HORIZON_DAYS = 35;
    private static final int MAX_TIMES = 96;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final EnumSet<AppointmentNotificationJobStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentNotificationJobStatus.PENDING,
                    AppointmentNotificationJobStatus.PROCESSING);

    private final ReminderScheduleRepository scheduleRepository;
    private final ReminderScheduleTimeRepository timeRepository;
    private final ReminderScheduleJobRepository jobRepository;
    private final Clock clock;

    @Autowired
    public ReminderScheduleServiceImpl(ReminderScheduleRepository scheduleRepository,
                                       ReminderScheduleTimeRepository timeRepository,
                                       ReminderScheduleJobRepository jobRepository) {
        this(scheduleRepository, timeRepository, jobRepository, Clock.systemUTC());
    }

    public ReminderScheduleServiceImpl(ReminderScheduleRepository scheduleRepository,
                                       ReminderScheduleTimeRepository timeRepository,
                                       ReminderScheduleJobRepository jobRepository,
                                       Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.timeRepository = timeRepository;
        this.jobRepository = jobRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReminderScheduleResponse create(UUID ownerUserId, CreateReminderScheduleRequest request) {
        if (ownerUserId == null || request == null) {
            throw invalid("Owner and schedule are required");
        }
        String zone = normalizeZone(request.timeZone());
        List<LocalTime> times = normalizeTimes(request.times());
        ReminderScheduleRecurrence recurrence = request.recurrence() == null
                ? ReminderScheduleRecurrence.NONE : request.recurrence();
        LocalDate start = request.startDate() == null
                ? LocalDate.now(clock.withZone(ZoneId.of(zone))) : request.startDate();
        validateDates(start, request.endDate());
        if ((request.active() == null || request.active())
                && recurrence == ReminderScheduleRecurrence.NONE
                && !hasFutureOccurrence(start, times, zone)) {
            throw invalid("A one-time reminder must have a future configured time");
        }
        ReminderSchedule schedule = ReminderSchedule.builder()
                .ownerUserId(ownerUserId)
                .title(requireTitle(request.title()))
                .timeZone(zone)
                .recurrence(recurrence)
                .startDate(start)
                .endDate(request.endDate())
                .active(request.active() == null || request.active())
                .revision(1L)
                .build();
        ReminderSchedule saved = scheduleRepository.save(schedule);
        replaceTimes(saved.getId(), times);
        materialize(saved);
        return toResponse(saved, times);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderScheduleResponse> list(UUID ownerUserId) {
        return scheduleRepository.findByOwnerUserIdOrderByStartDateAscCreatedAtDesc(ownerUserId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderScheduleResponse get(UUID ownerUserId, UUID scheduleId) {
        return toResponse(requireOwned(ownerUserId, scheduleId));
    }

    @Override
    @Transactional
    public ReminderScheduleResponse update(UUID ownerUserId, UUID scheduleId,
                                           UpdateReminderScheduleRequest request) {
        if (request == null) throw invalid("Schedule update is required");
        ReminderSchedule schedule = requireOwned(ownerUserId, scheduleId);
        List<LocalTime> times = request.times() == null
                ? currentTimes(schedule.getId()) : normalizeTimes(request.times());
        String zone = request.timeZone() == null
                ? schedule.getTimeZone() : normalizeZone(request.timeZone());
        String title = request.title() == null ? schedule.getTitle() : requireTitle(request.title());
        ReminderScheduleRecurrence recurrence = request.recurrence() == null
                ? schedule.getRecurrence() : request.recurrence();
        LocalDate start = request.startDate() == null ? schedule.getStartDate() : request.startDate();
        LocalDate end = Boolean.TRUE.equals(request.endDateSet())
                ? request.endDate()
                : request.endDate() == null ? schedule.getEndDate() : request.endDate();
        validateDates(start, end);
        if ((request.active() == null ? schedule.isActive() : request.active())
                && recurrence == ReminderScheduleRecurrence.NONE
                && !hasFutureOccurrence(start, times, zone)) {
            throw invalid("A one-time reminder must have a future configured time");
        }
        schedule.setTitle(title);
        schedule.setTimeZone(zone);
        schedule.setRecurrence(recurrence);
        schedule.setStartDate(start);
        schedule.setEndDate(end);
        if (request.active() != null) schedule.setActive(request.active());
        schedule.setRevision(schedule.getRevision() + 1L);
        ReminderSchedule saved = scheduleRepository.save(schedule);
        replaceTimes(saved.getId(), times);
        if (!saved.isActive()) {
            jobRepository.cancelActiveByScheduleId(saved.getId(), ACTIVE_STATUSES,
                    AppointmentNotificationJobStatus.CANCELLED, clock.instant());
        } else {
            jobRepository.cancelObsoleteRevisions(saved.getId(), saved.getRevision(), ACTIVE_STATUSES,
                    AppointmentNotificationJobStatus.CANCELLED, clock.instant());
            materialize(saved);
        }
        return toResponse(saved, times);
    }

    @Override
    @Transactional
    public void delete(UUID ownerUserId, UUID scheduleId) {
        ReminderSchedule schedule = requireOwned(ownerUserId, scheduleId);
        jobRepository.cancelActiveByScheduleId(schedule.getId(), ACTIVE_STATUSES,
                AppointmentNotificationJobStatus.CANCELLED, clock.instant());
        timeRepository.deleteByScheduleId(schedule.getId());
        scheduleRepository.delete(schedule);
    }

    /** Invoked by the gated horizon planner; it never changes the schedule revision. */
    @Transactional
    public void materializeForPlanner(ReminderSchedule schedule) {
        materialize(schedule);
    }

    private ReminderSchedule requireOwned(UUID ownerUserId, UUID scheduleId) {
        return scheduleRepository.findByIdAndOwnerUserId(scheduleId, ownerUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "REMINDER_SCHEDULE_NOT_FOUND", "Reminder schedule not found"));
    }

    private void replaceTimes(UUID scheduleId, List<LocalTime> times) {
        timeRepository.deleteByScheduleId(scheduleId);
        List<ReminderScheduleTime> rows = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            rows.add(ReminderScheduleTime.builder().scheduleId(scheduleId)
                    .localTime(times.get(i)).sortOrder(i).build());
        }
        timeRepository.saveAll(rows);
    }

    private List<LocalTime> currentTimes(UUID scheduleId) {
        List<LocalTime> values = timeRepository.findByScheduleIdOrderBySortOrderAscLocalTimeAsc(scheduleId)
                .stream().map(ReminderScheduleTime::getLocalTime).toList();
        if (values.isEmpty()) throw invalid("At least one reminder time is required");
        return values;
    }

    private void materialize(ReminderSchedule schedule) {
        if (!schedule.isActive()) return;
        ZoneId zone = ZoneId.of(schedule.getTimeZone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate first = schedule.getStartDate().isBefore(today) ? today : schedule.getStartDate();
        LocalDate last = schedule.getRecurrence() == ReminderScheduleRecurrence.NONE
                ? schedule.getStartDate() : first.plusDays(HORIZON_DAYS);
        if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(last)) {
            last = schedule.getEndDate();
        }
        if (last.isBefore(first)) return;
        List<LocalTime> times = currentTimes(schedule.getId());
        Instant now = clock.instant();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            for (LocalTime time : times) {
                Instant dueAt = date.atTime(time).atZone(zone).toInstant();
                if (!dueAt.isAfter(now)) continue;
                if (jobRepository.existsByScheduleIdAndScheduleRevisionAndOccurrenceDateAndLocalTime(
                        schedule.getId(), schedule.getRevision(), date, time)) continue;
                ReminderScheduleJob job = ReminderScheduleJob.builder()
                        .scheduleId(schedule.getId()).scheduleRevision(schedule.getRevision())
                        .occurrenceDate(date).localTime(time).timeZone(zone.getId()).dueAt(dueAt)
                        .status(AppointmentNotificationJobStatus.PENDING).attemptCount(0)
                        .nextAttemptAt(dueAt).createdAt(now).updatedAt(now).build();
                jobRepository.save(job);
            }
        }
    }

    private ReminderScheduleResponse toResponse(ReminderSchedule schedule) {
        return toResponse(schedule, currentTimes(schedule.getId()));
    }

    private ReminderScheduleResponse toResponse(ReminderSchedule schedule, List<LocalTime> times) {
        return new ReminderScheduleResponse(schedule.getId(), schedule.getTitle(),
                times.stream().map(TIME_FORMAT::format).toList(), schedule.getTimeZone(),
                schedule.getRecurrence(), schedule.getStartDate(), schedule.getEndDate(),
                schedule.isActive(), schedule.getRevision());
    }

    private static List<LocalTime> normalizeTimes(List<String> values) {
        if (values == null || values.isEmpty()) throw invalid("At least one reminder time is required");
        if (values.size() > MAX_TIMES) throw invalid("At most " + MAX_TIMES + " reminder times are allowed");
        TreeSet<LocalTime> sorted = new TreeSet<>();
        for (String value : values) {
            if (value == null || !value.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
                throw invalid("Reminder times must use strict HH:mm values");
            }
            try {
                sorted.add(LocalTime.parse(value, TIME_FORMAT));
            } catch (DateTimeParseException exception) {
                throw invalid("Reminder times must use strict HH:mm values");
            }
        }
        return List.copyOf(sorted);
    }

    private boolean hasFutureOccurrence(LocalDate start, List<LocalTime> times, String zoneId) {
        ZoneId zone = ZoneId.of(zoneId);
        Instant now = clock.instant();
        return times.stream().anyMatch(time -> start.atTime(time).atZone(zone).toInstant().isAfter(now));
    }

    private static String normalizeZone(String value) {
        if (value == null || value.isBlank()) throw invalid("timeZone is required");
        try {
            return ZoneId.of(value.trim()).getId();
        } catch (DateTimeException exception) {
            throw invalid("Invalid timeZone");
        }
    }

    private static String requireTitle(String value) {
        if (value == null || value.isBlank()) throw invalid("title is required");
        return value.trim();
    }

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || (end != null && end.isBefore(start))) {
            throw invalid("endDate must be on or after startDate");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "REMINDER_SCHEDULE_INVALID", message);
    }
}
