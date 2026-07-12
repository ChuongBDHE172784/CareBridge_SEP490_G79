package com.carebridge.backend.health.device.dto;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MetricType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ImportDeviceMetricRequest(
        @NotNull UUID journeyId,
        @NotNull MetricType metricType,
        @NotNull BigDecimal valueNumeric,
        BigDecimal valueSecondary,
        String unit,
        @NotNull Instant measuredAt,
        @NotNull DataSource sourceType,
        UUID deviceConnectionId,
        String note) {
}
