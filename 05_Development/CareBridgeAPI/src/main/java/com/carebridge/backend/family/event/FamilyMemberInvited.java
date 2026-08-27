package com.carebridge.backend.family.event;

import com.carebridge.backend.family.entity.InviteChannel;

import java.time.Instant;
import java.util.UUID;

public record FamilyMemberInvited(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String version,
        Payload payload,
        Metadata metadata
) {

    public record Payload(
            UUID careGroupId,
            UUID careGroupMemberId,
            UUID inviterId,
            InviteChannel channel,
            String inviteTokenHash,
            String invitedPhone,
            Instant expiresAt
    ) {}

    public record Metadata(
            UUID correlationId,
            String causedBy
    ) {}
}
