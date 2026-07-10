package com.carebridge.backend.family.event;

import java.time.Instant;
import java.util.UUID;

public record FamilyTaskAssigned(
        UUID    eventId,
        String  eventType,
        Instant occurredAt,
        String  version,
        Payload payload,
        Metadata metadata
) {

    public record Payload(
            UUID    careTaskId,
            UUID    careGroupId,
            UUID    assignedBy,
            UUID    assignedTo,
            String  title,
            Instant dueAt
    ) {}

    public record Metadata(
            UUID   correlationId,
            String causedBy
    ) {}
}
