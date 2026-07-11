package com.carebridge.backend.health.device.dto;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MetricType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ImportDeviceMetricResponse(
        UUID metricId,
        UUID journeyId,
        MetricType metricType,
        BigDecimal valueNumeric,
        BigDecimal valueSecondary,
        String unit,
        Instant measuredAt,
        DataSource sourceType,
        UUID sourceReferenceId) {
}
