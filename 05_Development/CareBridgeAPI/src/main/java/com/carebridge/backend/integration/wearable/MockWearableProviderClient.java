package com.carebridge.backend.integration.wearable;

import com.carebridge.backend.health.device.entity.HealthDeviceConnection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockWearableProviderClient implements WearableProviderClient {

    @Override
    public List<RawMeasurement> fetchMeasurements(HealthDeviceConnection connection) {
        return List.of();
    }
}
