package com.carebridge.backend.health.device.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceConnectionResponse(
        UUID id,
        String providerName,
        String deviceName,
        String status,
        Instant consentGrantedAt,
        Instant lastSyncedAt,
        Instant createdAt,
        Instant updatedAt) {
}
