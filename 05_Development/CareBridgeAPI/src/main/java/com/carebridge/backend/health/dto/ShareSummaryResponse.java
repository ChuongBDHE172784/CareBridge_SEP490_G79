package com.carebridge.backend.health.dto;

import java.time.Instant;
import java.util.UUID;

public record ShareSummaryResponse(
        UUID bookingId,
        UUID summaryId,
        Instant sharedAt
) {}
