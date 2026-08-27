package com.carebridge.backend.emergency.event;

import java.time.Instant;
import java.util.UUID;

public record FamilyAlertSent(
        UUID eventId,
        UUID sessionId,
        UUID userId,
        int recipientCount,
        boolean locationIncluded,
        Instant sentAt
) {}
