package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface UnifiedTodayTaskService {
    TodayTasksResponse getTodayTasks(UUID actorUserId, LocalDate date, String timezoneHeader);
}
