package com.carebridge.backend.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateHealthSummaryRequest(

        UUID journeyId,

        UUID babyId,

        @NotBlank(message = "HEALTH-001: summaryPeriod is required")
        @Pattern(regexp = "^(24H|7D|CONSULTATION)$",
                message = "HEALTH-002: summaryPeriod must be 24H, 7D, or CONSULTATION")
        String summaryPeriod,

        LocalDate periodStart,

        LocalDate periodEnd,

        @NotBlank(message = "HEALTH-001: summaryJson is required")
        String summaryJson
) {}
