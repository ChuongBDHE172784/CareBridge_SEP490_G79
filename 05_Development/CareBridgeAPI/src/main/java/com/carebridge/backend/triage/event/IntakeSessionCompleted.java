package com.carebridge.backend.triage.event;

import com.carebridge.backend.triage.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record IntakeSessionCompleted(
        UUID eventId,
        UUID sessionId,
        UUID userId,
        RiskLevel riskLevel,
        Instant completedAt
) {}
