package com.carebridge.backend.health.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricDataPoint {

    private UUID metricId;
    private Instant measuredAt;
    private BigDecimal valueNumeric;
    private BigDecimal valueSecondary;
    private String sourceType;
    private String note;
    private Map<String, Object> context;
    private Instant periodStart;
    private Instant periodEnd;
    private String qualityLabel;
}
