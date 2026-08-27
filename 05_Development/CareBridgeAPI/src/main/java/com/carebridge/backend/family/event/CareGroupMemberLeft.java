package com.carebridge.backend.family.event;

import java.time.Instant;
import java.util.UUID;

public record CareGroupMemberLeft(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String version,
        Payload payload,
        Metadata metadata) {

    public record Payload(UUID careGroupId, UUID careGroupMemberId, UUID userId, int reassignedTaskCount) {}

    public record Metadata(UUID correlationId, String source) {}
}
