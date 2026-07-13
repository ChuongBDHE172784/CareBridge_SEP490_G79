package com.carebridge.backend.safety.service;

import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

public interface IFallDetectionService {
    ImuMonitoringSessionResponse enable(UUID userId, String sensitivityLevel);
    void disable(UUID userId);
    SafetyEventResponse processImuData(UUID userId, ImuDataPayload payload);
    List<SafetyEventResponse> listSafetyEvents(UUID userId, Pageable pageable);
    SafetyEventResponse confirmSafetyCheck(UUID userId, UUID eventId, String note);
    SafetyEventResponse reportFalsePositive(UUID userId, UUID eventId, String note);
    void sendEmergencyAlert(UUID userId, UUID eventId);
}
