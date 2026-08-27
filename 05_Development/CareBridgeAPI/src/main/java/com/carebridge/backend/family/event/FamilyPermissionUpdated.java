package com.carebridge.backend.family.event;

import com.carebridge.backend.family.dto.FamilyPermission;

import java.time.Instant;
import java.util.UUID;

public record FamilyPermissionUpdated(
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
            UUID updatedBy,
            FamilyPermission previousPermissions,
            FamilyPermission newPermissions
    ) {}

    public record Metadata(
            UUID correlationId,
            String causedBy
    ) {}
}
