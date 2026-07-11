package com.carebridge.backend.health.device.event;

import java.time.Instant;
import java.util.UUID;

public record DeviceDisconnected(UUID eventId, Instant timestamp, UUID connectionId, UUID userId, String providerName) {
}
