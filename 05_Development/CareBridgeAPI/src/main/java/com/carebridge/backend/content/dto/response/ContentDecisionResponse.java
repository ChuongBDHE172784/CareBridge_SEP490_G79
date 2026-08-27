package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStatus;
import java.time.Instant;
import java.util.UUID;

public record ContentDecisionResponse(
        UUID id,
        ContentStatus previousStatus,
        ContentStatus newStatus,
        Integer versionNoAtDecision,
        UUID decidedByAdminId,
        String reason,
        Instant decidedAt
) {}
