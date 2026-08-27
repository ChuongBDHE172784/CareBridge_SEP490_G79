package com.carebridge.backend.ai.event;

import java.time.Instant;
import java.util.UUID;

public record StructuredIntakeExtracted(
        UUID eventId,
        UUID sessionId,
        UUID structuredDataId,
        boolean emergencyFlag,
        Instant extractedAt
) {}
