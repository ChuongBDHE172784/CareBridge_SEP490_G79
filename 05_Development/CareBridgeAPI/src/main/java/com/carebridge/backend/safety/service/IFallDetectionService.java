package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import java.util.UUID;

public interface IFallDetectionService {
    ImuMonitoringSessionResponse enable(UUID userId, String sensitivityLevel);
    void disable(UUID userId);
    SafetyEventResponse processImuData(UUID userId, ImuDataPayload payload);
}
