package com.carebridge.backend.content.dto.response;

import java.util.UUID;

/** One moderator-facing dossier entry, grouped by sanctioned account. */
public record AccountViolationSummaryItemResponse(
        UUID targetUserId,
        String targetUserName,
        long violationCount,
        AccountViolationHistoryItemResponse latestAction
) {}
