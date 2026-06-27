package com.carebridge.backend.ai.event;

import java.time.Instant;
import java.util.UUID;

public record EmergencyEscalationTriggered(
        UUID eventId,
        UUID sessionId,
        UUID userId,
        String triggerSource,
        Instant triggeredAt
) {}
