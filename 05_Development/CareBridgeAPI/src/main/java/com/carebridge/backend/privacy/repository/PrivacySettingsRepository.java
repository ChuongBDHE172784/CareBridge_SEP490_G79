package com.carebridge.backend.privacy.repository;

import com.carebridge.backend.privacy.entity.PrivacySettings;
import com.carebridge.backend.privacy.entity.ProfileVisibility;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Persists privacy preferences inside the canonical users.settings_jsonb document. */
@Repository
@RequiredArgsConstructor
public class PrivacySettingsRepository {
    private static final String KEY = "privacy";
    private final UserRepository userRepository;

    public Optional<PrivacySettings> findByUserId(UUID userId) {
        return userRepository.findById(userId).map(this::fromUser)
                .filter(settings -> settings.getId() != null);
    }

    public PrivacySettings save(PrivacySettings value) {
        User user = userRepository.findById(value.getUserId()).orElseThrow();
        Map<String, Object> settings = user.getSettings() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(user.getSettings());
        settings.put(KEY, Map.of(
                "profileVisibility", value.getProfileVisibility().name(),
                "locationSharingEnabled", value.isLocationSharingEnabled(),
                "analyticsConsent", value.isAnalyticsConsent(),
                "dataExportOptOut", value.isDataExportOptOut()));
        user.setSettings(settings);
        userRepository.save(user);
        if (value.getId() == null) value.setId(value.getUserId());
        if (value.getCreatedAt() == null) value.setCreatedAt(Instant.now());
        value.setUpdatedAt(Instant.now());
        return value;
    }

    @SuppressWarnings("unchecked")
    private PrivacySettings fromUser(User user) {
        Object raw = user.getSettings() == null ? null : user.getSettings().get(KEY);
        if (!(raw instanceof Map<?, ?> map)) return PrivacySettings.defaultSettings(user.getId());
        return PrivacySettings.builder().id(user.getId()).userId(user.getId())
                .profileVisibility(ProfileVisibility.valueOf(String.valueOf(
                        map.containsKey("profileVisibility") ? map.get("profileVisibility") : "FRIENDS_ONLY")))
                .locationSharingEnabled(flag(map, "locationSharingEnabled"))
                .analyticsConsent(flag(map, "analyticsConsent"))
                .dataExportOptOut(flag(map, "dataExportOptOut"))
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }

    private boolean flag(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
