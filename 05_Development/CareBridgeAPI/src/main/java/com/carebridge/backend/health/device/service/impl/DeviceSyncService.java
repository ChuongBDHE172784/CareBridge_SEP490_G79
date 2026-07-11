package com.carebridge.backend.health.device.service.impl;

import com.carebridge.backend.health.device.dto.DeviceSyncResultResponse;
import com.carebridge.backend.health.device.entity.DeviceConnectionStatus;
import com.carebridge.backend.health.device.entity.DeviceMeasurement;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import com.carebridge.backend.health.device.event.DeviceDataSynced;
import com.carebridge.backend.health.device.event.DeviceSyncFailed;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import com.carebridge.backend.health.device.repository.IDeviceMeasurementRepository;
import com.carebridge.backend.health.device.repository.IHealthDeviceConnectionRepository;
import com.carebridge.backend.health.device.service.IDeviceSyncService;
import com.carebridge.backend.integration.wearable.RawMeasurement;
import com.carebridge.backend.integration.wearable.WearableProviderClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceSyncService implements IDeviceSyncService {

    private final IHealthDeviceConnectionRepository connectionRepository;
    private final IDeviceMeasurementRepository measurementRepository;
    private final WearableProviderClient providerClient;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DeviceSyncResultResponse syncNow(UUID connectionId, UUID userId) {
        HealthDeviceConnection connection = connectionRepository.findByConnectionIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new DeviceOperationException(
                        "SYNC-001", HttpStatus.NOT_FOUND, "Device connection not found"));
        return syncConnection(connection, "MANUAL", true);
    }

    @Override
    @Transactional
    public List<DeviceSyncResultResponse> syncAllActiveConnections() {
        List<DeviceSyncResultResponse> results = new ArrayList<>();
        for (HealthDeviceConnection connection : connectionRepository.findByStatus(DeviceConnectionStatus.ACTIVE)) {
            try {
                results.add(syncConnection(connection, "SCHEDULED", false));
            } catch (RuntimeException ex) {
                Instant now = Instant.now();
                eventPublisher.publishEvent(new DeviceSyncFailed(
                        UUID.randomUUID(), now, connection.getConnectionId(), connection.getUserId(),
                        "Provider sync failed", true, "SCHEDULED"));
                results.add(new DeviceSyncResultResponse(
                        connection.getConnectionId(), 0, 1, List.of("Provider sync failed"), now, "SCHEDULED"));
            }
        }
        return results;
    }

    private DeviceSyncResultResponse syncConnection(
            HealthDeviceConnection connection, String triggerType, boolean throwOnProviderFailure) {
        if (connection.getStatus() != DeviceConnectionStatus.ACTIVE || connection.getConsentGrantedAt() == null) {
            throw new DeviceOperationException(
                    "SYNC-002", HttpStatus.CONFLICT, "Device connection is not active or consent missing");
        }

        Instant now = Instant.now();
        List<String> skippedReasons = new ArrayList<>();
        int syncedCount = 0;
        try {
            for (RawMeasurement raw : providerClient.fetchMeasurements(connection)) {
                if (raw.sourceRecordId() != null
                        && measurementRepository.existsByConnectionIdAndSourceRecordId(
                                connection.getConnectionId(), raw.sourceRecordId())) {
                    skippedReasons.add("Duplicate source record skipped");
                    continue;
                }
                measurementRepository.save(DeviceMeasurement.builder()
                        .connectionId(connection.getConnectionId())
                        .measurementType(raw.measurementType())
                        .valueNumeric(raw.valueNumeric())
                        .valueSecondary(raw.valueSecondary())
                        .unit(raw.unit())
                        .measuredAt(raw.measuredAt())
                        .sourceRecordId(raw.sourceRecordId())
                        .qualityLabel(raw.qualityLabel())
                        .rawMetadataJson(raw.rawMetadataJson())
                        .build());
                syncedCount++;
            }
        } catch (RuntimeException ex) {
            eventPublisher.publishEvent(new DeviceSyncFailed(
                    UUID.randomUUID(), now, connection.getConnectionId(), connection.getUserId(),
                    "Provider sync failed", true, triggerType));
            if (throwOnProviderFailure) {
                throw new DeviceOperationException("SYNC-003", HttpStatus.BAD_GATEWAY, "Provider sync failed");
            }
            skippedReasons.add("Provider sync failed");
        }

        connection.setLastSyncedAt(now);
        connectionRepository.save(connection);
        DeviceSyncResultResponse response = new DeviceSyncResultResponse(
                connection.getConnectionId(), syncedCount, skippedReasons.size(), skippedReasons, now, triggerType);
        eventPublisher.publishEvent(new DeviceDataSynced(
                UUID.randomUUID(), now, connection.getConnectionId(), connection.getUserId(),
                syncedCount, skippedReasons.size(), triggerType));
        return response;
    }
}
