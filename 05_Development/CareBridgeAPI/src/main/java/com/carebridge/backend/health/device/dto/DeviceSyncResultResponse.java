package com.carebridge.backend.health.device.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeviceSyncResultResponse(
        UUID connectionId,
        int syncedCount,
        int skippedCount,
        List<String> skippedReasons,
        Instant syncedAt,
        String triggerType) {
}
