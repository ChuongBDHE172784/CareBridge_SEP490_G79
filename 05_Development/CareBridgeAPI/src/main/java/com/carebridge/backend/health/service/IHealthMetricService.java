package com.carebridge.backend.health.service;

import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.dto.MetricResponse;
import com.carebridge.backend.health.dto.MetricTrendResponse;
import com.carebridge.backend.health.dto.UpdateMetricRequest;
import com.carebridge.backend.health.entity.MetricType;

import java.time.Instant;
import java.util.UUID;

public interface IHealthMetricService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (METRIC-003/403) if not owner */
    MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId);

    /** UC188: Soft-deletes an ACTIVE metric owned by the caller.
     * @throws com.carebridge.backend.common.exception.BusinessException (METRIC-001/404, METRIC-002/404, METRIC-003/403)
     */
    void deleteMetric(UUID metricId, UUID callerId);

    /** UC25: Add a new metric to an ACTIVE journey. Async AI insight (fail-open).
     * @throws com.carebridge.backend.common.exception.BusinessException (METRIC-001/404, METRIC-002/403, METRIC-003/400, METRIC-004/400, METRIC-005/400)
     */
    MetricResponse addMetric(UUID userId, UUID journeyId, AddMetricRequest request);

    /** UC26: Update mutable fields of an ACTIVE metric within the 24-hour edit window.
     * @throws com.carebridge.backend.common.exception.BusinessException (METRIC-010/404, METRIC-011/404, METRIC-012/400, METRIC-013/403)
     */
    MetricResponse updateMetric(UUID userId, UUID journeyId, UUID metricId, UpdateMetricRequest request);

    /** UC27: View metric trend for a journey and metric type over a date range. Empty list = 200 OK.
     * @throws com.carebridge.backend.common.exception.BusinessException (METRIC-020/404, METRIC-021/403, METRIC-022/400)
     */
    MetricTrendResponse getMetricTrend(UUID userId, UUID journeyId, MetricType metricType, Instant from, Instant to);
}
