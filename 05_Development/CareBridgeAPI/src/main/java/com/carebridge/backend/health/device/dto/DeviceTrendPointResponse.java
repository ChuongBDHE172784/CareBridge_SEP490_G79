package com.carebridge.backend.health.device.dto;

import com.carebridge.backend.health.entity.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DeviceTrendPointResponse(
        UUID metricId,
        Instant measuredAt,
        BigDecimal valueNumeric,
        BigDecimal valueSecondary,
        String unit,
        DataSource sourceType,
        String sourceLabel,
        boolean accuracyWarning) {
}
