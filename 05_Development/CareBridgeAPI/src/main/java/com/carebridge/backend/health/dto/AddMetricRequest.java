package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MetricType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AddMetricRequest {

    @NotNull
    private MetricType metricType;

    @NotNull
    private BigDecimal valueNumeric;

    private BigDecimal valueSecondary;

    @Size(max = 30)
    private String unit;

    @NotNull
    private Instant measuredAt;

    private DataSource sourceType;

    @Size(max = 2000)
    private String note;
}
