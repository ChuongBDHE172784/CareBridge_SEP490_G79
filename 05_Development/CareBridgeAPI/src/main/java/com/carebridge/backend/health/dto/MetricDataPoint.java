package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class MetricDataPoint {

    private Instant measuredAt;
    private BigDecimal valueNumeric;
    private BigDecimal valueSecondary;
    private String note;
}
