package com.carebridge.backend.health.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShareSummaryRequest(

        @NotNull(message = "HEALTH-007: summaryId is required")
        UUID summaryId,

        @NotNull(message = "HEALTH-007: bookingId is required")
        UUID bookingId
) {}
