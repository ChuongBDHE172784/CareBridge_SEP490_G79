package com.carebridge.backend.health.device.dto;

import com.carebridge.backend.health.entity.MetricType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record DeviceTrendQuery(
        @NotNull UUID journeyId,
        @NotNull MetricType metricType,
        @NotNull Instant from,
        @NotNull Instant to) {
}
