package com.carebridge.backend.safety.event;

import java.time.Instant;
import java.util.UUID;

public record SafetyConfigChanged(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String version,
        Payload payload,
        Metadata metadata
) {
    public record Payload(UUID userId, boolean fallDetectionEnabled, String sensitivityLevel) {}

    public record Metadata(UUID correlationId, String initiatedBy) {}
}
