package com.carebridge.backend.privacy.repository;

import com.carebridge.backend.privacy.entity.PrivacySettings;
import com.carebridge.backend.privacy.entity.ProfileVisibility;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persists privacy preferences inside the canonical users.settings_jsonb document. */
@Repository
@RequiredArgsConstructor
public class PrivacySettingsRepository {
    private static final String KEY = "privacy";
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public Optional<PrivacySettings> findByUserId(UUID userId) {
        return userRepository.findById(userId).map(this::fromUser)
                .filter(settings -> settings.getId() != null);
    }

    public Optional<PrivacySettings> findByUserIdForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId).map(this::fromUser)
                .filter(settings -> settings.getId() != null);
    }

    public PrivacySettings save(PrivacySettings value) {
        patchFields(
                value.getUserId(),
                value.getProfileVisibility(),
                value.isLocationSharingEnabled(),
                value.isAnalyticsConsent(),
                value.isDataExportOptOut());
        if (value.getId() == null) value.setId(value.getUserId());
        if (value.getCreatedAt() == null) value.setCreatedAt(Instant.now());
        value.setUpdatedAt(Instant.now());
        return value;
    }

    public void patchFields(
            UUID userId,
            ProfileVisibility profileVisibility,
            Boolean locationSharingEnabled,
            Boolean analyticsConsent,
            Boolean dataExportOptOut) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                   SET settings_jsonb = jsonb_set(
                       CASE WHEN jsonb_typeof(settings_jsonb) = 'object'
                            THEN settings_jsonb ELSE '{}'::jsonb END,
                       '{privacy}',
                       CASE WHEN jsonb_typeof(settings_jsonb -> 'privacy') = 'object'
                            THEN settings_jsonb -> 'privacy' ELSE '{}'::jsonb END
                       || jsonb_strip_nulls(jsonb_build_object(
                           'profileVisibility', CAST(? AS text),
                           'locationSharingEnabled', CAST(? AS boolean),
                           'analyticsConsent', CAST(? AS boolean),
                           'dataExportOptOut', CAST(? AS boolean)
                       )),
                       true
                   ),
                       updated_at = now()
                 WHERE user_id = ?
                """,
                profileVisibility == null ? null : profileVisibility.name(),
                locationSharingEnabled,
                analyticsConsent,
                dataExportOptOut,
                userId);
        if (updated != 1) {
            throw new NoSuchElementException("User not found: " + userId);
        }
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
