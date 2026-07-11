package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MetricDataPoint {

    private UUID metricId;
    private Instant measuredAt;
    private BigDecimal valueNumeric;
    private BigDecimal valueSecondary;
    private String sourceType;
    private String note;
}
