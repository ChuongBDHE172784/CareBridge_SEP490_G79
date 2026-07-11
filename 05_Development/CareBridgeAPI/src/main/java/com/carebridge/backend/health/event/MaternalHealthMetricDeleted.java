package com.carebridge.backend.health.event;

import java.time.Instant;
import java.util.UUID;

public record MaternalHealthMetricDeleted(
        UUID metricId,
        UUID journeyId,
        UUID actorUserId,
        Instant occurredAt
) {
}
