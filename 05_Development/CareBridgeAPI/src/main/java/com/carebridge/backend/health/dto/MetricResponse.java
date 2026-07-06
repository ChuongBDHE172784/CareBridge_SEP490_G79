package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MetricResponse {

    private UUID metricId;
    private UUID journeyId;
    private String metricType;
    private BigDecimal valueNumeric;
    private BigDecimal valueSecondary;
    private String unit;
    private Instant measuredAt;
    private String sourceType;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
    private String aiInsight;
    private boolean redFlagAlert;
}
