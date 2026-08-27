package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MetricType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

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

    /** Structured P0 context, for example glucose measurementContext. */
    private Map<String, Object> context = new LinkedHashMap<>();

    /** Fetal movement/session period start. */
    private Instant periodStart;

    /** Fetal movement/session period end. */
    private Instant periodEnd;

    /** Optional client-provided metric definition version. */
    private Integer definitionVersion;
}
