package com.carebridge.backend.checklist.history.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ChecklistHistoryItemResponse(
        UUID checklistInstanceId,
        UUID templateVersionId,
        String templateName,
        String stage,
        String targetSubject,
        String careContextType,
        UUID careContextId,
        String careContextLabel,
        LocalDate windowStart,
        LocalDate windowEnd,
        Instant historicalAt,
        String historyReasonCode,
        List<ChecklistHistoryTaskResponse> tasks) {
}
