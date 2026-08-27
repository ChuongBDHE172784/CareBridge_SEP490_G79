package com.carebridge.backend.health.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class UpdateMetricRequest {

    private BigDecimal valueNumeric;

    private BigDecimal valueSecondary;

    @Size(max = 30)
    private String unit;

    private Instant measuredAt;

    @Size(max = 2000)
    private String note;

    private Map<String, Object> context;

    private Instant periodStart;

    private Instant periodEnd;
}
