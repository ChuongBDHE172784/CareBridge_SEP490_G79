package com.carebridge.backend.family.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CareTaskUpdated(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String version,
        Payload payload,
        Metadata metadata) {

    public record Payload(UUID careTaskId, UUID careGroupId, UUID previousAssignee,
                          UUID newAssignee, List<String> changedFields) {}

    public record Metadata(UUID correlationId, String source) {}
}
