package com.carebridge.backend.health.device.service;

import com.carebridge.backend.health.device.dto.DeviceTrendQuery;
import com.carebridge.backend.health.device.dto.DeviceTrendResponse;
import java.util.UUID;

public interface IDeviceTrendService {

    DeviceTrendResponse getTrend(DeviceTrendQuery query, UUID userId);
}
