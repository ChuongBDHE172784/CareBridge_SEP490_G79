package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.SensitivityLevel;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and writes safety configuration on the typed {@code users} columns.
 *
 * <p>R8 cutover (V3 §3.9): {@code safety_configs} is no longer read. Callers keep
 * working with {@link SafetyMonitoringConfig} — the shape the DTOs and events are
 * built from — but instances handed out here are detached projections of a user
 * row, not managed entities of the retired table.
 *
 * <p>The legacy {@code safety_configs} mirror was removed at R12; the users columns
 * are now the only store.
 */
@Component
@RequiredArgsConstructor
public class SafetyConfigStore {

    private final UserRepository userRepository;

    /**
     * Every existing user now has a configuration, because the columns carry the
     * same defaults the service used to synthesise. Empty means "no such user".
     */
    @Transactional(readOnly = true)
    public Optional<SafetyMonitoringConfig> findByUserId(UUID userId) {
        return userRepository.findById(userId).map(SafetyConfigStore::project);
    }

    @Transactional
    public SafetyMonitoringConfig save(SafetyMonitoringConfig config) {
        UUID userId = config.getUserId();
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalStateException("Safety config for unknown user " + userId));

        user.setFallDetectionEnabled(config.isFallDetectionEnabled());
        user.setFallDetectionSensitivityLevel(
                config.getSensitivityLevel() == null
                        ? SensitivityLevel.MEDIUM.name()
                        : config.getSensitivityLevel().name());
        user.setEmergencyAutoAlert(config.isEmergencyAutoAlert());
        user.setSafetyLocationSharingEnabled(config.isLocationSharingEnabled());
        user.setEmergencyCountdownSeconds(config.getCountdownSeconds());
        user.setSensorPermissionGranted(config.isSensorPermissionGranted());
        user.setSensorPermissionRecordedAt(config.getSensorPermissionRecordedAt());
        user.setSafetyConfigUpdatedAt(
                config.getUpdatedAt() == null ? Instant.now() : config.getUpdatedAt());
        user.setSafetyConfigUpdatedBy(config.getUpdatedBy());
        userRepository.save(user);

        return project(user);
    }

    private static SafetyMonitoringConfig project(User user) {
        SafetyMonitoringConfig config = new SafetyMonitoringConfig();
        // The configuration is 1:1 with the user now, so the user id is its identity.
        config.setId(user.getId());
        config.setUserId(user.getId());
        config.setFallDetectionEnabled(user.isFallDetectionEnabled());
        config.setSensitivityLevel(parseSensitivity(user.getFallDetectionSensitivityLevel()));
        config.setEmergencyAutoAlert(user.isEmergencyAutoAlert());
        config.setLocationSharingEnabled(user.isSafetyLocationSharingEnabled());
        config.setCountdownSeconds(user.getEmergencyCountdownSeconds());
        config.setSensorPermissionGranted(user.isSensorPermissionGranted());
        config.setSensorPermissionRecordedAt(user.getSensorPermissionRecordedAt());
        config.setUpdatedAt(user.getSafetyConfigUpdatedAt());
        config.setUpdatedBy(user.getSafetyConfigUpdatedBy());
        return config;
    }

    private static SensitivityLevel parseSensitivity(String value) {
        if (value == null || value.isBlank()) {
            return SensitivityLevel.MEDIUM;
        }
        try {
            return SensitivityLevel.valueOf(value);
        } catch (IllegalArgumentException exception) {
            // The column CHECK keeps this unreachable; defaulting beats failing a
            // fall-detection read on a value the database should never have allowed.
            return SensitivityLevel.MEDIUM;
        }
    }
}
