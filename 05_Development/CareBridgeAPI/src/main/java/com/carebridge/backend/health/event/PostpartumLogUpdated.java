package com.carebridge.backend.health.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostpartumLogUpdated(
        UUID logId,
        UUID journeyId,
        UUID actorUserId,
        List<String> changedFields,
        Instant occurredAt
) {
}
