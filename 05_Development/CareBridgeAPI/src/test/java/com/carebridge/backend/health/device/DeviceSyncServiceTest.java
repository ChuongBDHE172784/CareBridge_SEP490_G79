package com.carebridge.backend.health.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.DeviceMeasurement;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IDeviceMeasurementRepository;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.impl.DeviceSyncService;
import com.carebridge.backend.integration.wearable.RawMeasurement;
import com.carebridge.backend.integration.wearable.WearableProviderClient;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeviceSyncServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private IHealthDeviceConnectionRepository connectionRepository;

    @Mock
    private IDeviceMeasurementRepository measurementRepository;

    @Mock
    private WearableProviderClient providerClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeviceSyncService service;

    @Test
    void syncNowRechecksActiveStatusAndConsentBeforeProviderCall() {
        HealthDeviceConnection revoked = connection(DeviceConnectionStatus.REVOKED, Instant.now());
        when(connectionRepository.findByConnectionIdAndUserId(revoked.getConnectionId(), USER_ID))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.syncNow(revoked.getConnectionId(), USER_ID))
                .isInstanceOf(DeviceOperationException.class)
                .hasMessageContaining("not active or consent missing");
        verify(providerClient, never()).fetchMeasurements(any());
    }

    @Test
    void syncNowSkipsDuplicateSourceRecordsAndUpdatesLastSyncedAt() {
        HealthDeviceConnection active = connection(DeviceConnectionStatus.ACTIVE, Instant.now());
        UUID duplicateSourceId = UUID.randomUUID();
        UUID newSourceId = UUID.randomUUID();
        when(connectionRepository.findByConnectionIdAndUserId(active.getConnectionId(), USER_ID))
                .thenReturn(Optional.of(active));
        when(providerClient.fetchMeasurements(active)).thenReturn(List.of(
                raw(duplicateSourceId),
                raw(newSourceId)));
        when(measurementRepository.existsByConnectionIdAndSourceRecordId(active.getConnectionId(), duplicateSourceId))
                .thenReturn(true);
        when(measurementRepository.existsByConnectionIdAndSourceRecordId(active.getConnectionId(), newSourceId))
                .thenReturn(false);

        var response = service.syncNow(active.getConnectionId(), USER_ID);

        assertThat(response.syncedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(active.getLastSyncedAt()).isNotNull();
        verify(measurementRepository).save(any(DeviceMeasurement.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    private static HealthDeviceConnection connection(DeviceConnectionStatus status, Instant consentGrantedAt) {
        return HealthDeviceConnection.builder()
                .connectionId(UUID.randomUUID())
                .userId(USER_ID)
                .providerName("SMARTWATCH_GENERIC")
                .tokenReference("secret-token-reference")
                .consentGrantedAt(consentGrantedAt)
                .status(status)
                .build();
    }

    private static RawMeasurement raw(UUID sourceRecordId) {
        return new RawMeasurement(
                sourceRecordId,
                "SPO2",
                new BigDecimal("98"),
                null,
                "%",
                Instant.parse("2026-07-01T12:00:00Z"),
                "OK",
                "{}");
    }
}

