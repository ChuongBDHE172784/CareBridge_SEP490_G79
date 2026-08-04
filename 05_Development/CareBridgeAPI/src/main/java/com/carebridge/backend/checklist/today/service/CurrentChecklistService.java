package com.carebridge.backend.checklist.today.service;

import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import java.time.LocalDate;
import java.util.UUID;

/** Canonical current-stage checklist read. Unlike the legacy Today endpoint this
 * contract never exposes reminder schedules or family care tasks. */
public interface CurrentChecklistService {
    CurrentChecklistResponse getCurrentTasks(UUID actorUserId, LocalDate date, String timezoneHeader);
}
