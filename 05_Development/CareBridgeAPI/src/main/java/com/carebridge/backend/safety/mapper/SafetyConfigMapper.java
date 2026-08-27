package com.carebridge.backend.safety.mapper;

import com.carebridge.backend.safety.dto.response.SafetyConfigResponse;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import org.springframework.stereotype.Component;

@Component
public class SafetyConfigMapper {

    public SafetyConfigResponse toResponse(SafetyMonitoringConfig config) {
        if (config == null) {
            return null;
        }
        return SafetyConfigResponse.builder()
                .id(config.getId())
                .userId(config.getUserId())
                .fallDetectionEnabled(config.isFallDetectionEnabled())
                .sensitivityLevel(config.getSensitivityLevel() != null
                        ? config.getSensitivityLevel().name()
                        : null)
                .emergencyAutoAlert(config.isEmergencyAutoAlert())
                .locationSharingEnabled(config.isLocationSharingEnabled())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
