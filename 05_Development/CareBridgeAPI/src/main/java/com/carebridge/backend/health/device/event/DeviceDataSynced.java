package com.carebridge.backend.health.device.event;

import java.time.Instant;
import java.util.UUID;

public record DeviceDataSynced(UUID eventId, Instant timestamp, UUID connectionId, UUID userId, int syncedCount,
                               int skippedCount, String triggerType) {
}
