package com.carebridge.backend.health.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.health.device.dto.ImportDeviceMetricRequest;
import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.impl.DeviceDataImportService;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeviceDataImportServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID JOURNEY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private MaternalHealthMetricRepository metricRepository;

    @Mock
    private IHealthDeviceConnectionRepository connectionRepository;

    @Mock
    private MotherJourneyRepository journeyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeviceDataImportService service;

    @Test
    void importManualMetricStoresNoSourceReference() {
        when(journeyRepository.existsByIdAndOwnerUserId(JOURNEY_ID, USER_ID)).thenReturn(true);
        when(metricRepository.save(any())).thenAnswer(invocation -> {
            MaternalHealthMetric metric = invocation.getArgument(0);
            metric.setId(UUID.randomUUID());
            return metric;
        });

        service.importMetric(new ImportDeviceMetricRequest(
                JOURNEY_ID, MetricType.SPO2, new BigDecimal("98"), null, "%", Instant.now(),
                DataSource.MANUAL, null, "home reading"), USER_ID);

        ArgumentCaptor<MaternalHealthMetric> captor = ArgumentCaptor.forClass(MaternalHealthMetric.class);
        verify(metricRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceReferenceId()).isNull();
        assertThat(captor.getValue().getSourceType()).isEqualTo(DataSource.MANUAL);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void importDeviceMetricRequiresActiveOwnedConnection() {
        UUID connectionId = UUID.randomUUID();
        when(journeyRepository.existsByIdAndOwnerUserId(JOURNEY_ID, USER_ID)).thenReturn(true);
        when(connectionRepository.findByConnectionIdAndUserId(connectionId, USER_ID))
                .thenReturn(Optional.of(HealthDeviceConnection.builder()
                        .connectionId(connectionId)
                        .userId(USER_ID)
                        .providerName("SMARTWATCH_GENERIC")
                        .status(DeviceConnectionStatus.REVOKED)
                        .build()));

        assertThatThrownBy(() -> service.importMetric(new ImportDeviceMetricRequest(
                JOURNEY_ID, MetricType.HEART_RATE, new BigDecimal("90"), null, "bpm", Instant.now(),
                DataSource.DEVICE, connectionId, null), USER_ID))
                .isInstanceOf(DeviceOperationException.class)
                .hasMessageContaining("Device connection not active");
        verify(metricRepository, never()).save(any());
    }

    @Test
    void importRejectsOutOfRangeValueWithNeutralMessage() {
        when(journeyRepository.existsByIdAndOwnerUserId(JOURNEY_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.importMetric(new ImportDeviceMetricRequest(
                JOURNEY_ID, MetricType.SPO2, new BigDecimal("120"), null, "%", Instant.now(),
                DataSource.MANUAL, null, null), USER_ID))
                .isInstanceOf(DeviceOperationException.class)
                .hasMessageContaining("Value out of allowed range")
                .hasMessageNotContaining("danger");
        verify(metricRepository, never()).save(any());
    }
}

