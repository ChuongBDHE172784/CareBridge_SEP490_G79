package com.carebridge.backend.health.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetricTrendResponse {

    private String metricType;
    private String unit;
    private List<MetricDataPoint> dataPoints;
    private String disclaimer;
}
