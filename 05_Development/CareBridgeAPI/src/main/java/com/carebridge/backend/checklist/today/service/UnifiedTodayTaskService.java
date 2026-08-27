package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.model.TaskKind;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface UnifiedTodayTaskService {
    TodayTasksResponse getTodayTasks(UUID actorUserId, LocalDate date, String timezoneHeader);

    /** Restrict provider execution for an explicit product surface. */
    default TodayTasksResponse getTodayTasks(
            UUID actorUserId, LocalDate date, String timezoneHeader, Set<TaskKind> kinds) {
        return getTodayTasks(actorUserId, date, timezoneHeader, kinds, true);
    }

    default TodayTasksResponse getTodayTasks(
            UUID actorUserId, LocalDate date, String timezoneHeader,
            Set<TaskKind> kinds, boolean reconcile) {
        return getTodayTasks(actorUserId, date, timezoneHeader);
    }
}
