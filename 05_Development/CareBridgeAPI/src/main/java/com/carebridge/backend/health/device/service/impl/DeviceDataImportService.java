package com.carebridge.backend.health.device.service.impl;

import com.carebridge.backend.health.device.dto.ImportDeviceMetricRequest;
import com.carebridge.backend.health.device.dto.ImportDeviceMetricResponse;
import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.event.DeviceDataImported;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.IDeviceDataImportService;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceDataImportService implements IDeviceDataImportService {

    private static final Map<MetricType, Range> RANGES = Map.of(
            MetricType.SLEEP_DURATION, new Range("0", "24"),
            MetricType.STEPS_COUNT, new Range("0", "100000"),
            MetricType.SPO2, new Range("50", "100"),
            MetricType.BLOOD_PRESSURE_SYSTOLIC, new Range("50", "250"),
            MetricType.BLOOD_PRESSURE_DIASTOLIC, new Range("30", "180"));

    private final MaternalHealthMetricRepository metricRepository;
    private final IHealthDeviceConnectionRepository connectionRepository;
    private final MotherJourneyRepository journeyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ImportDeviceMetricResponse importMetric(ImportDeviceMetricRequest request, UUID userId) {
        verifyJourneyOwnership(request.journeyId(), userId);
        verifyRange(request.metricType(), request.valueNumeric());

        UUID sourceReferenceId = null;
        if (request.sourceType() == DataSource.DEVICE) {
            HealthDeviceConnection connection = connectionRepository.findByConnectionIdAndUserId(
                            request.deviceConnectionId(), userId)
                    .filter(c -> c.getStatus() == DeviceConnectionStatus.ACTIVE)
                    .orElseThrow(() -> new DeviceOperationException(
                            "DEVICE-102", HttpStatus.CONFLICT, "Device connection not active"));
            sourceReferenceId = connection.getConnectionId();
        }

        MaternalHealthMetric metric = MaternalHealthMetric.builder()
                .journeyId(request.journeyId())
                .metricType(request.metricType())
                .valueNumeric(request.valueNumeric())
                .valueSecondary(request.valueSecondary())
                .unit(request.unit())
                .measuredAt(request.measuredAt())
                .sourceType(request.sourceType())
                .sourceReferenceId(sourceReferenceId)
                .note(request.note())
                .status(MetricStatus.ACTIVE)
                .build();

        MaternalHealthMetric saved = metricRepository.save(metric);
        eventPublisher.publishEvent(new DeviceDataImported(
                UUID.randomUUID(), Instant.now(), saved.getId(), userId, saved.getJourneyId(), saved.getMetricType()));
        return new ImportDeviceMetricResponse(
                saved.getId(),
                saved.getJourneyId(),
                saved.getMetricType(),
                saved.getValueNumeric(),
                saved.getValueSecondary(),
                saved.getUnit(),
                saved.getMeasuredAt(),
                saved.getSourceType(),
                saved.getSourceReferenceId());
    }

    private void verifyJourneyOwnership(UUID journeyId, UUID userId) {
        if (!journeyRepository.existsByIdAndOwnerUserId(journeyId, userId)) {
            throw new DeviceOperationException("DEVICE-004", HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private void verifyRange(MetricType metricType, BigDecimal value) {
        Range range = RANGES.get(metricType);
        if (range == null) {
            throw new DeviceOperationException("DEVICE-100", HttpStatus.BAD_REQUEST, "Validation failed");
        }
        if (value.compareTo(range.min()) < 0 || value.compareTo(range.max()) > 0) {
            throw new DeviceOperationException("DEVICE-101", HttpStatus.BAD_REQUEST, "Value out of allowed range");
        }
    }

    private record Range(BigDecimal min, BigDecimal max) {
        Range(String min, String max) {
            this(new BigDecimal(min), new BigDecimal(max));
        }
    }
}
