package com.carebridge.backend.safety;

import com.carebridge.backend.safety.dto.request.SafetyConfigRequest;
import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.event.SafetyConfigChanged;
import java.time.Instant;
import java.util.UUID;

class SafetyConfigTestFactory {

    static SafetyMonitoringConfig makeConfig() {
        return SafetyMonitoringConfig.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .fallDetectionEnabled(false)
                .sensitivityLevel(SensitivityLevel.MEDIUM)
                .emergencyAutoAlert(true)
                .updatedAt(Instant.parse("2026-06-27T08:00:00Z"))
                .build();
    }

    static SafetyConfigRequest makeRequest() {
        return SafetyConfigRequest.builder()
                .fallDetectionEnabled(true)
                .sensitivityLevel("MEDIUM")
                .emergencyAutoAlert(true)
                .build();
    }

    static ImuMonitoringSession makeActiveSession() {
        return ImuMonitoringSession.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .status(ImuSessionStatus.ACTIVE)
                .sensitivityLevel("MEDIUM")
                .startedAt(Instant.parse("2026-06-27T08:00:00Z"))
                .build();
    }

    static SafetyConfigChanged makeConfigChangedEvent(boolean enabled) {
        return new SafetyConfigChanged(
                UUID.randomUUID(),
                "SafetyConfigChanged",
                Instant.now(),
                "1.0",
                new SafetyConfigChanged.Payload(
                        UUID.fromString("00000000-0000-0000-0000-000000000010"), enabled, "MEDIUM"),
                new SafetyConfigChanged.Metadata(UUID.randomUUID(), "user-001"));
    }
}
