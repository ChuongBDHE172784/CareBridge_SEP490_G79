package com.carebridge.backend.health.device.service;

import com.carebridge.backend.health.device.dto.ConnectDeviceRequest;
import com.carebridge.backend.health.device.dto.DeviceConnectionResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDeviceConnectionService {

    DeviceConnectionResponse connect(ConnectDeviceRequest request, UUID userId);

    DeviceConnectionResponse disconnect(UUID connectionId, UUID userId);

    Optional<DeviceConnectionResponse> getActiveConnection(UUID userId);

    List<DeviceConnectionResponse> listConnections(UUID userId);
}
