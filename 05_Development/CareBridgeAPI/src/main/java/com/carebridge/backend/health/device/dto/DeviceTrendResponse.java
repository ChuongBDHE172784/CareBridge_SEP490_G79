package com.carebridge.backend.health.device.dto;

import com.carebridge.backend.health.entity.MetricType;
import java.util.List;
import java.util.UUID;

public record DeviceTrendResponse(
        UUID journeyId,
        MetricType metricType,
        String unit,
        boolean hasAnyData,
        List<DeviceTrendPointResponse> points) {
}
