package com.carebridge.backend.health.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricDetailResponse {

    private UUID id;
    private UUID journeyId;
    private String metricType;
    private BigDecimal valueNumeric;
    private BigDecimal valueSecondary;
    private String unit;
    private Instant measuredAt;
    private String sourceType;
    private String note;
    private Instant createdAt;
    private Map<String, Object> context;
    private Instant periodStart;
    private Instant periodEnd;
    private String qualityLabel;
    private String disclaimer;
    private Integer definitionVersion;
}
