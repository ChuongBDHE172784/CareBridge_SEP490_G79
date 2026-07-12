package com.carebridge.backend.integration.wearable;

import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import java.util.List;

public interface WearableProviderClient {

    List<RawMeasurement> fetchMeasurements(HealthDeviceConnection connection);
}
