package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.distribution.EnsureEligibleChecklistAssignmentsService;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskCounts;
import com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskSections;
import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskTimeBucket;
import com.carebridge.backend.checklist.today.provider.TodayTaskProvider;
import java.time.DateTimeException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedTodayTaskServiceImpl implements UnifiedTodayTaskService {
    private static final String DEFAULT_ZONE = "Asia/Ho_Chi_Minh";
    private static final int HORIZON_DAYS = 7;

    private final List<TodayTaskProvider> providers;
    private final TodayTaskContextLabelResolver labelResolver;
    private final EnsureEligibleChecklistAssignmentsService ensureAssignments;
    private final Clock clock;

    @Autowired
    public UnifiedTodayTaskServiceImpl(
            List<TodayTaskProvider> providers,
            TodayTaskContextLabelResolver labelResolver,
            EnsureEligibleChecklistAssignmentsService ensureAssignments) {
        this(providers, labelResolver, ensureAssignments, Clock.systemUTC());
    }

    public UnifiedTodayTaskServiceImpl(List<TodayTaskProvider> providers, Clock clock) {
        this(providers, null, null, clock);
    }

    public UnifiedTodayTaskServiceImpl(
            List<TodayTaskProvider> providers,
            TodayTaskContextLabelResolver labelResolver) {
        this(providers, labelResolver, null, Clock.systemUTC());
    }

    public UnifiedTodayTaskServiceImpl(
            List<TodayTaskProvider> providers,
            TodayTaskContextLabelResolver labelResolver,
            EnsureEligibleChecklistAssignmentsService ensureAssignments,
            Clock clock) {
        this.providers = List.copyOf(providers);
        this.labelResolver = labelResolver;
        this.ensureAssignments = ensureAssignments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TodayTasksResponse getTodayTasks(UUID actorUserId, LocalDate date, String timezoneHeader) {
        ZoneId zone = resolveZone(timezoneHeader);
        LocalDate effectiveDate = date == null ? LocalDate.ofInstant(clock.instant(), zone) : date;
        UUID correlationId = UUID.randomUUID();
        if (ensureAssignments != null) {
            ensureAssignments.ensureEligibleAssignments(actorUserId, effectiveDate, zone, correlationId);
        }
        Instant dayStart = effectiveDate.atStartOfDay(zone).toInstant();
        Instant nextDayStart = effectiveDate.plusDays(1).atStartOfDay(zone).toInstant();
        Instant horizonEnd = effectiveDate.plusDays(HORIZON_DAYS + 1L).atStartOfDay(zone).toInstant();

        List<TodayTaskCandidate> candidates = new ArrayList<>();
        for (TodayTaskProvider provider : providers) {
            candidates.addAll(provider.findAuthorizedTasks(actorUserId));
        }
        Map<TodayTaskContextLabelResolver.ContextKey, TodayTaskContextLabelResolver.Labels> labels =
                labelResolver == null ? Map.of() : labelResolver.resolve(candidates);

        Map<String, TodayTaskItemResponse> unique = new LinkedHashMap<>();
        for (TodayTaskCandidate candidate : candidates) {
                TaskTimeBucket bucket = bucket(candidate.status(), candidate.dueAt(), candidate.terminalAt(),
                        dayStart, nextDayStart, horizonEnd);
                if (bucket == null) {
                    continue;
                }
                TodayTaskContextLabelResolver.Labels taskLabels = labels.get(
                        TodayTaskContextLabelResolver.ContextKey.from(candidate));
                TodayTaskItemResponse item = new TodayTaskItemResponse(
                        candidate.taskKind(), candidate.taskId(), candidate.instanceId(),
                        candidate.templateVersionId(), candidate.careGroupId(),
                        candidate.careContextType(), candidate.careContextId(),
                        taskLabels == null ? null : taskLabels.careGroupLabel(),
                        taskLabels == null ? null : taskLabels.careContextLabel(), candidate.title(),
                        candidate.targetSubject(), candidate.origin(), candidate.status(), bucket,
                        candidate.allowedActions(), candidate.dueAt(), candidate.reminderType());
                unique.putIfAbsent(candidate.taskKind() + ":" + candidate.taskId(), item);
        }

        List<TodayTaskItemResponse> overdue = section(unique, TaskTimeBucket.OVERDUE);
        List<TodayTaskItemResponse> today = section(unique, TaskTimeBucket.TODAY);
        List<TodayTaskItemResponse> upcoming = section(unique, TaskTimeBucket.UPCOMING);
        List<TodayTaskItemResponse> unscheduled = section(unique, TaskTimeBucket.UNSCHEDULED);
        return new TodayTasksResponse(clock.instant(), zone.getId(), HORIZON_DAYS,
                new TodayTaskSections(overdue, today, upcoming, unscheduled),
                new TodayTaskCounts(overdue.size(), today.size(), upcoming.size(), unscheduled.size()),
                correlationId);
    }

    private static List<TodayTaskItemResponse> section(
            Map<String, TodayTaskItemResponse> unique, TaskTimeBucket bucket) {
        Comparator<TodayTaskItemResponse> order = Comparator
                .comparing(TodayTaskItemResponse::dueAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(item -> item.taskKind().name())
                .thenComparing(TodayTaskItemResponse::taskId);
        return unique.values().stream()
                .filter(item -> item.timeBucket() == bucket)
                .sorted(order)
                .toList();
    }

    private static TaskTimeBucket bucket(String status, Instant dueAt, Instant terminalAt,
                                         Instant dayStart, Instant nextDayStart, Instant horizonEnd) {
        if ("CANCELLED".equals(status)) {
            return null;
        }
        boolean terminal = "COMPLETED".equals(status) || "SKIPPED".equals(status)
                || "DONE".equals(status);
        if (dueAt == null) {
            if (!terminal) {
                return TaskTimeBucket.UNSCHEDULED;
            }
            return terminalAt != null
                    && !terminalAt.isBefore(dayStart)
                    && terminalAt.isBefore(nextDayStart)
                    ? TaskTimeBucket.TODAY
                    : null;
        }
        if (dueAt.isBefore(dayStart)) {
            return terminal ? null : TaskTimeBucket.OVERDUE;
        }
        if (dueAt.isBefore(nextDayStart)) {
            return TaskTimeBucket.TODAY;
        }
        if (dueAt.isBefore(horizonEnd)) {
            return terminal ? null : TaskTimeBucket.UPCOMING;
        }
        return null;
    }

    private static ZoneId resolveZone(String header) {
        if (header == null || header.isBlank()) {
            return ZoneId.of(DEFAULT_ZONE);
        }
        try {
            return ZoneId.of(header);
        } catch (DateTimeException ignored) {
            return ZoneId.of(DEFAULT_ZONE);
        }
    }
}
