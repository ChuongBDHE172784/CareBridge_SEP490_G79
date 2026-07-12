package com.carebridge.backend.health.device.event;

import java.time.Instant;
import java.util.UUID;

public record DeviceSyncFailed(UUID eventId, Instant timestamp, UUID connectionId, UUID userId, String reason,
                               boolean retryable, String triggerType) {
}
