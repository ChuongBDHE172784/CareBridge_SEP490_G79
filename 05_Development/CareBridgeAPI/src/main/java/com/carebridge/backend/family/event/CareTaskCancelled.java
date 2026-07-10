package com.carebridge.backend.family.event;

import java.time.Instant;
import java.util.UUID;

public record CareTaskCancelled(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String version,
        Payload payload,
        Metadata metadata) {

    public record Payload(UUID careTaskId, UUID careGroupId, UUID assignedTo,
                          UUID assignedBy, UUID cancelledBy, String title) {}

    public record Metadata(UUID correlationId, String source) {}
}
