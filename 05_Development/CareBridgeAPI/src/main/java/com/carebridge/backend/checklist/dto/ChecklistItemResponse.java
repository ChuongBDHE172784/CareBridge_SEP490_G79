package com.carebridge.backend.checklist.dto;

import java.time.Instant;
import java.util.UUID;

public record ChecklistItemResponse(
        UUID itemId,
        UUID ownerUserId,
        UUID journeyId,
        UUID babyId,
        UUID templateItemId,
        String itemText,
        String category,
        boolean completed,
        Instant completedAt,
        int itemOrder,
        Instant createdAt
) {}
