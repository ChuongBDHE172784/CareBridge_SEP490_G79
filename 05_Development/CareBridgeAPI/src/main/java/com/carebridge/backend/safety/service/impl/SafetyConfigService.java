package com.carebridge.backend.safety.service.impl;

import com.carebridge.backend.safety.SensitivityLevel;
import com.carebridge.backend.safety.dto.request.SafetyConfigRequest;
import com.carebridge.backend.safety.dto.response.SafetyConfigResponse;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.event.SafetyConfigChanged;
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import com.carebridge.backend.safety.service.ISafetyConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import com.carebridge.backend.safety.exception.SafetyException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
@Transactional
public class SafetyConfigService implements ISafetyConfigService {

    private final ISafetyConfigRepository configRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public SafetyConfigResponse configure(SafetyConfigRequest request, UUID userId) {
        SafetyMonitoringConfig config = configRepository.findByUserId(userId)
                .orElseGet(SafetyMonitoringConfig::new);
        config.setUserId(userId);
        config.setFallDetectionEnabled(request.getFallDetectionEnabled());
        config.setSensitivityLevel(SensitivityLevel.valueOf(request.getSensitivityLevel()));
        config.setEmergencyAutoAlert(request.getEmergencyAutoAlert());
        if (request.getCountdownSeconds() != null) {
            if (request.getCountdownSeconds() != 15
                    && request.getCountdownSeconds() != 30
                    && request.getCountdownSeconds() != 60) {
                throw new SafetyException(HttpStatus.BAD_REQUEST, "SAFETY-008",
                        "countdownSeconds must be 15, 30, or 60");
            }
            config.setCountdownSeconds(request.getCountdownSeconds());
        } else if (config.getCountdownSeconds() == 0) {
            config.setCountdownSeconds(30);
        }
        if (request.getSensorPermissionGranted() != null) {
            config.setSensorPermissionGranted(request.getSensorPermissionGranted());
            config.setSensorPermissionRecordedAt(Instant.now());
        }
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(userId);
        SafetyMonitoringConfig saved = configRepository.save(config);
        eventPublisher.publishEvent(new SafetyConfigChanged(
                UUID.randomUUID(),
                "SafetyConfigChanged",
                Instant.now(),
                "1.0",
                new SafetyConfigChanged.Payload(userId, request.getFallDetectionEnabled(), request.getSensitivityLevel()),
                new SafetyConfigChanged.Metadata(UUID.randomUUID(), userId.toString())));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SafetyConfigResponse getConfig(UUID userId) {
        return configRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> SafetyConfigResponse.builder()
                        .userId(userId)
                        .fallDetectionEnabled(false)
                        .sensitivityLevel("MEDIUM")
                        .emergencyAutoAlert(true)
                        .countdownSeconds(30)
                        .sensorPermissionGranted(false)
                        .build());
    }

    private SafetyConfigResponse toResponse(SafetyMonitoringConfig config) {
        return SafetyConfigResponse.builder()
                .id(config.getId())
                .userId(config.getUserId())
                .fallDetectionEnabled(config.isFallDetectionEnabled())
                .sensitivityLevel(config.getSensitivityLevel().name())
                .emergencyAutoAlert(config.isEmergencyAutoAlert())
                .countdownSeconds(config.getCountdownSeconds())
                .sensorPermissionGranted(config.isSensorPermissionGranted())
                .sensorPermissionRecordedAt(config.getSensorPermissionRecordedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
