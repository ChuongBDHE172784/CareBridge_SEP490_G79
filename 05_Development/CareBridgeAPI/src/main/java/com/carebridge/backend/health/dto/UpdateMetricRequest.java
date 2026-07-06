package com.carebridge.backend.health.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class UpdateMetricRequest {

    private BigDecimal valueNumeric;

    private BigDecimal valueSecondary;

    @Size(max = 30)
    private String unit;

    private Instant measuredAt;

    @Size(max = 2000)
    private String note;
}
