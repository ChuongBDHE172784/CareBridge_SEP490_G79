package com.carebridge.backend.health.device.service.impl;

import com.carebridge.backend.health.device.dto.DeviceTrendPointResponse;
import com.carebridge.backend.health.device.dto.DeviceTrendQuery;
import com.carebridge.backend.health.device.dto.DeviceTrendResponse;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.IDeviceTrendService;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTrendService implements IDeviceTrendService {

    private final MaternalHealthMetricRepository metricRepository;
    private final IHealthDeviceConnectionRepository connectionRepository;
    private final MotherJourneyRepository journeyRepository;

    @Override
    @Transactional(readOnly = true)
    public DeviceTrendResponse getTrend(DeviceTrendQuery query, UUID userId) {
        if (query.from().isAfter(query.to())) {
            throw new DeviceOperationException("DEVICE-301", HttpStatus.BAD_REQUEST, "Invalid trend query parameters");
        }
        if (!journeyRepository.existsById(query.journeyId())) {
            throw new DeviceOperationException("DEVICE-302", HttpStatus.NOT_FOUND, "Journey not found");
        }
        if (!journeyRepository.existsByIdAndOwnerUserId(query.journeyId(), userId)) {
            throw new DeviceOperationException("DEVICE-304", HttpStatus.FORBIDDEN, "Insufficient permissions");
        }

        List<MaternalHealthMetric> metrics = metricRepository
                .findByJourneyIdAndMetricTypeAndMeasuredAtBetweenAndStatusOrderByMeasuredAtAsc(
                        query.journeyId(), query.metricType(), query.from(), query.to(), MetricStatus.ACTIVE);
        List<DeviceTrendPointResponse> points = metrics.stream()
                .map(this::toPoint)
                .toList();
        String unit = points.isEmpty() ? null : points.getFirst().unit();
        return new DeviceTrendResponse(query.journeyId(), query.metricType(), unit, !points.isEmpty(), points);
    }

    private DeviceTrendPointResponse toPoint(MaternalHealthMetric metric) {
        return new DeviceTrendPointResponse(
                metric.getId(),
                metric.getMeasuredAt(),
                metric.getValueNumeric(),
                metric.getValueSecondary(),
                metric.getUnit(),
                metric.getSourceType(),
                resolveSourceLabel(metric),
                false);
    }

    private String resolveSourceLabel(MaternalHealthMetric metric) {
        if (metric.getSourceType() != DataSource.DEVICE || metric.getSourceReferenceId() == null) {
            return "Manual entry";
        }
        return connectionRepository.findById(metric.getSourceReferenceId())
                .map(this::connectionLabel)
                .orElse("Device");
    }

    private String connectionLabel(HealthDeviceConnection connection) {
        if (connection.getDeviceName() == null || connection.getDeviceName().isBlank()) {
            return connection.getProviderName();
        }
        return connection.getDeviceName();
    }
}
