package com.carebridge.backend.checklist.today.dto;

import java.time.Instant;
import java.util.UUID;

public record TodayTasksResponse(
        Instant asOf,
        String zoneId,
        int horizonDays,
        TodayTaskSections sections,
        TodayTaskCounts counts,
        UUID correlationId) {
}
