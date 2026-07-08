package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MetricTrendResponse {

    private String metricType;
    private String unit;
    private List<MetricDataPoint> dataPoints;
}
