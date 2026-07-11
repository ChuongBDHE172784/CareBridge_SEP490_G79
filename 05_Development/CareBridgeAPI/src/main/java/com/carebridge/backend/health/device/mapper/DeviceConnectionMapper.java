package com.carebridge.backend.health.device.mapper;

import com.carebridge.backend.health.device.dto.DeviceConnectionResponse;
import com.carebridge.backend.health.device.entity.HealthDeviceConnection;

public final class DeviceConnectionMapper {

    private DeviceConnectionMapper() {
    }

    public static DeviceConnectionResponse toResponse(HealthDeviceConnection entity) {
        return new DeviceConnectionResponse(
                entity.getConnectionId(),
                entity.getProviderName(),
                entity.getDeviceName(),
                entity.getStatus().name(),
                entity.getConsentGrantedAt(),
                entity.getLastSyncedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
