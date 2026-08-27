package com.carebridge.backend.checklist.history.dto;

import java.time.Instant;
import java.util.UUID;

public record ChecklistHistoryTaskResponse(
        UUID taskId,
        String title,
        String status,
        Instant completedAt,
        Instant skippedAt,
        Instant cancelledAt,
        Integer displayOrder,
        boolean required) {
}
