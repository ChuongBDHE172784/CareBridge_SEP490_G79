package com.carebridge.backend.health.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HealthSummaryResponse(
        UUID summaryId,
        UUID ownerUserId,
        UUID journeyId,
        UUID babyId,
        String summaryPeriod,
        LocalDate periodStart,
        LocalDate periodEnd,
        String summaryJson,
        String generatedBy,
        String status,
        Instant createdAt
) {}
