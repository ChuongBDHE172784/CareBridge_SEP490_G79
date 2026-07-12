package com.carebridge.backend.health.device.service;

import com.carebridge.backend.health.device.dto.DeviceSyncResultResponse;
import java.util.List;
import java.util.UUID;

public interface IDeviceSyncService {

    DeviceSyncResultResponse syncNow(UUID connectionId, UUID userId);

    List<DeviceSyncResultResponse> syncAllActiveConnections();
}
