package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistSections;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistTaskResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse;
import com.carebridge.backend.checklist.today.dto.TodayTaskSections;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CurrentChecklistServiceImpl implements CurrentChecklistService {
    private final UnifiedTodayTaskService unifiedTodayTaskService;

    public CurrentChecklistServiceImpl(UnifiedTodayTaskService unifiedTodayTaskService) {
        this.unifiedTodayTaskService = unifiedTodayTaskService;
    }

    @Override
    public CurrentChecklistResponse getCurrentTasks(
            UUID actorUserId, LocalDate date, String timezoneHeader) {
        // The generic route is still readable by FAMILY for compatibility, but
        // authenticated FAMILY reads stay read-only. Internal callers without a
        // security context retain the Mother-compatible reconciliation behavior.
        boolean reconcile = SecurityUtils.currentAuthorities().isEmpty()
                || SecurityUtils.hasRole("MOTHER");
        TodayTasksResponse response = unifiedTodayTaskService.getTodayTasks(
                actorUserId, date, timezoneHeader,
                Set.of(com.carebridge.backend.checklist.today.model.TaskKind.CHECKLIST), reconcile);
        TodayTaskSections sections = response.sections();
        CurrentChecklistSections currentSections = new CurrentChecklistSections(
                map(sections.overdue()), map(sections.today()),
                map(sections.upcoming()), map(sections.unscheduled()));
        return new CurrentChecklistResponse(response.asOf(), response.zoneId(),
                response.horizonDays(), currentSections, response.counts(),
                response.correlationId(), response.sequence());
    }

    private static java.util.List<CurrentChecklistTaskResponse> map(
            java.util.List<TodayTaskItemResponse> items) {
        return items.stream().map(item -> new CurrentChecklistTaskResponse(
                item.taskId(), item.instanceId(), item.templateVersionId(), item.careGroupId(),
                item.careContextType(), item.careContextId(), item.careGroupLabel(),
                item.careContextLabel(), item.title(), item.targetSubject(), item.origin(),
                item.status(), item.timeBucket(), item.allowedActions(), item.dueAt())).toList();
    }
}
