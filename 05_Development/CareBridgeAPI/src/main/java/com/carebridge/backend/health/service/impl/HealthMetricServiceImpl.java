package com.carebridge.backend.health.service.impl;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.MetricDetailResponse;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.health.service.IHealthMetricService;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HealthMetricServiceImpl implements IHealthMetricService {

    private final MaternalHealthMetricRepository metricRepository;
    private final MotherJourneyRepository journeyRepository;

    @Override
    public MetricDetailResponse getMetricDetail(UUID metricId, UUID callerId) {
        // C3: DELETED returns 404
        MaternalHealthMetric metric = metricRepository.findByIdAndStatus(metricId, MetricStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-001",
                        "Metric not found or deleted: " + metricId));

        // C1: ownership via journey — metric → journey → owner_user_id
        MotherJourney journey = journeyRepository.findById(metric.getJourneyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "METRIC-002",
                        "Parent journey not found for metric: " + metricId));

        if (!journey.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "METRIC-003",
                    "Access denied to health metric");
        }

        // C2: no diagnosis in response (BR-SAFETY)
        return MetricDetailResponse.builder()
                .id(metric.getId())
                .journeyId(metric.getJourneyId())
                .metricType(metric.getMetricType().name())
                .valueNumeric(metric.getValueNumeric())
                .valueSecondary(metric.getValueSecondary())
                .unit(metric.getUnit())
                .measuredAt(metric.getMeasuredAt())
                .sourceType(metric.getSourceType() != null ? metric.getSourceType().name() : null)
                .note(metric.getNote())
                .createdAt(metric.getCreatedAt())
                .build();
    }
}
