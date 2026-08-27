package com.carebridge.backend.reminder.service;

import com.carebridge.backend.reminder.dto.TodayTaskItem;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public interface ITodayTaskService {

    /** UC49 — returns reminders (PENDING/SNOOZED) + care tasks (OPEN) due today, sorted by priority */
    List<TodayTaskItem> getTodayTasks(UUID callerId, ZoneId timezone);
}
