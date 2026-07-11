package com.carebridge.backend.health.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PostpartumLogDeleted(
        UUID logId,
        UUID journeyId,
        LocalDate logDate,
        UUID actorUserId,
        Instant occurredAt
) {
}
