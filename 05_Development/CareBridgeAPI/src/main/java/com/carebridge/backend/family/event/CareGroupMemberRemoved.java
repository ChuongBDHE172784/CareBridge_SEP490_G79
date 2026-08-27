package com.carebridge.backend.family.event;

import java.time.Instant;
import java.util.UUID;

public record CareGroupMemberRemoved(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String version,
        Payload payload,
        Metadata metadata) {

    public record Payload(UUID careGroupId, UUID careGroupMemberId, UUID targetUserId, UUID removedBy) {}

    public record Metadata(UUID correlationId, String source) {}
}
