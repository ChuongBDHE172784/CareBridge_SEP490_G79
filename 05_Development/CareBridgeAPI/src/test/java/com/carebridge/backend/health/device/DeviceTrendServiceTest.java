package com.carebridge.backend.health.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.health.device.dto.DeviceTrendQuery;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.impl.DeviceTrendService;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MaternalHealthMetric;
import com.carebridge.backend.health.entity.MetricStatus;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.repository.MaternalHealthMetricRepository;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceTrendServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID JOURNEY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private MaternalHealthMetricRepository metricRepository;

    @Mock
    private IHealthDeviceConnectionRepository connectionRepository;

    @Mock
    private MotherJourneyRepository journeyRepository;

    @InjectMocks
    private DeviceTrendService service;

    @Test
    void getTrendReturnsSourceLabelAndNoAccuracyWarningForProposedAdr() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-02T00:00:00Z");
        UUID connectionId = UUID.randomUUID();
        when(journeyRepository.existsById(JOURNEY_ID)).thenReturn(true);
        when(journeyRepository.existsByIdAndOwnerUserId(JOURNEY_ID, USER_ID)).thenReturn(true);
        when(metricRepository.findByJourneyIdAndMetricTypeAndMeasuredAtBetweenAndStatusOrderByMeasuredAtAsc(
                        JOURNEY_ID, MetricType.SPO2, from, to, MetricStatus.ACTIVE))
                .thenReturn(List.of(metric(connectionId)));
        when(connectionRepository.findById(connectionId)).thenReturn(Optional.of(HealthDeviceConnection.builder()
                .connectionId(connectionId)
                .userId(USER_ID)
                .providerName("SMARTWATCH_GENERIC")
                .deviceName("Mi Band 8")
                .build()));

        var response = service.getTrend(new DeviceTrendQuery(JOURNEY_ID, MetricType.SPO2, from, to), USER_ID);

        assertThat(response.hasAnyData()).isTrue();
        assertThat(response.points()).hasSize(1);
        assertThat(response.points().getFirst().sourceLabel()).isEqualTo("Mi Band 8");
        assertThat(response.points().getFirst().accuracyWarning()).isFalse();
    }

    @Test
    void getTrendEmptyResultReturns200Shape() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-02T00:00:00Z");
        when(journeyRepository.existsById(JOURNEY_ID)).thenReturn(true);
        when(journeyRepository.existsByIdAndOwnerUserId(JOURNEY_ID, USER_ID)).thenReturn(true);
        when(metricRepository.findByJourneyIdAndMetricTypeAndMeasuredAtBetweenAndStatusOrderByMeasuredAtAsc(
                        JOURNEY_ID, MetricType.SPO2, from, to, MetricStatus.ACTIVE))
                .thenReturn(List.of());

        var response = service.getTrend(new DeviceTrendQuery(JOURNEY_ID, MetricType.SPO2, from, to), USER_ID);

        assertThat(response.hasAnyData()).isFalse();
        assertThat(response.points()).isEmpty();
    }

    @Test
    void getTrendRejectsWrongOwnerBeforeReadingMetrics() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-02T00:00:00Z");
        when(journeyRepository.existsById(JOURNEY_ID)).thenReturn(true);
        when(journeyRepository.existsByIdAndOwnerUserId(JOURNEY_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getTrend(new DeviceTrendQuery(JOURNEY_ID, MetricType.SPO2, from, to), USER_ID))
                .isInstanceOf(DeviceOperationException.class)
                .hasMessageContaining("Insufficient permissions");
        verify(metricRepository, never())
                .findByJourneyIdAndMetricTypeAndMeasuredAtBetweenAndStatusOrderByMeasuredAtAsc(
                        JOURNEY_ID, MetricType.SPO2, from, to, MetricStatus.ACTIVE);
    }

    private static MaternalHealthMetric metric(UUID sourceReferenceId) {
        return MaternalHealthMetric.builder()
                .id(UUID.randomUUID())
                .journeyId(JOURNEY_ID)
                .metricType(MetricType.SPO2)
                .valueNumeric(new BigDecimal("98"))
                .unit("%")
                .measuredAt(Instant.parse("2026-07-01T12:00:00Z"))
                .sourceType(DataSource.DEVICE)
                .sourceReferenceId(sourceReferenceId)
                .status(MetricStatus.ACTIVE)
                .build();
    }
}

