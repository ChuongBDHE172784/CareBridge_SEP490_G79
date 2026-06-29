package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.MetricDetailResponse;

import java.util.UUID;

public interface IHealthMetricService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (METRIC-003/403) if not owner */
    MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId);
}
