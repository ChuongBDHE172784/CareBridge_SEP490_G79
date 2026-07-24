package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.NotificationPreference;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persists notification preferences inside the canonical users.settings_jsonb document. */
@Repository
@RequiredArgsConstructor
public class NotificationPreferenceRepository {
    private static final String KEY = "notifications";
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<NotificationPreference> findByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        Map<String, Object> values = section(user);
        List<NotificationPreference> result = new ArrayList<>();
        values.forEach((type, value) -> {
            if (value instanceof Map<?, ?> channels) {
                toPreference(userId, type, channels).ifPresent(result::add);
            }
        });
        return result;
    }

    public Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, NotificationType type) {
        return findByUserId(userId).stream().filter(p -> p.getNotificationType() == type).findFirst();
    }

    public NotificationPreference save(NotificationPreference preference) {
        patchChannels(
                preference.getUserId(),
                preference.getNotificationType(),
                preference.getPushEnabled(),
                preference.getEmailEnabled(),
                preference.getInAppEnabled());
        if (preference.getPreferenceId() == null) preference.setPreferenceId(UUID.randomUUID());
        if (preference.getCreatedAt() == null) preference.setCreatedAt(Instant.now());
        preference.setUpdatedAt(Instant.now());
        return preference;
    }

    public void patchChannels(
            UUID userId,
            NotificationType type,
            Boolean pushEnabled,
            Boolean emailEnabled,
            Boolean inAppEnabled) {
        int updated = jdbcTemplate.update("""
                UPDATE users
                   SET settings_jsonb = jsonb_set(
                       jsonb_set(
                           CASE WHEN jsonb_typeof(settings_jsonb) = 'object'
                                THEN settings_jsonb ELSE '{}'::jsonb END,
                           '{notifications}',
                           CASE WHEN jsonb_typeof(settings_jsonb -> 'notifications') = 'object'
                                THEN settings_jsonb -> 'notifications' ELSE '{}'::jsonb END,
                           true
                       ),
                       ARRAY['notifications', CAST(? AS text)],
                       CASE WHEN jsonb_typeof(
                                    (CASE WHEN jsonb_typeof(settings_jsonb -> 'notifications') = 'object'
                                          THEN settings_jsonb -> 'notifications' ELSE '{}'::jsonb END)
                                    -> CAST(? AS text)
                                ) = 'object'
                            THEN (CASE WHEN jsonb_typeof(settings_jsonb -> 'notifications') = 'object'
                                      THEN settings_jsonb -> 'notifications' ELSE '{}'::jsonb END)
                                 -> CAST(? AS text)
                            ELSE '{}'::jsonb END
                       || jsonb_strip_nulls(jsonb_build_object(
                           'pushEnabled', CAST(? AS boolean),
                           'emailEnabled', CAST(? AS boolean),
                           'inAppEnabled', CAST(? AS boolean)
                       )),
                       true
                   ),
                       updated_at = now()
                 WHERE user_id = ?
                """,
                type.name(), type.name(), type.name(),
                pushEnabled, emailEnabled, inAppEnabled, userId);
        if (updated != 1) {
            throw new NoSuchElementException("User not found: " + userId);
        }
    }

    public void deleteByUserId(UUID userId) {
        jdbcTemplate.update("""
                UPDATE users
                   SET settings_jsonb =
                       (CASE WHEN jsonb_typeof(settings_jsonb) = 'object'
                             THEN settings_jsonb ELSE '{}'::jsonb END) - 'notifications',
                       updated_at = now()
                 WHERE user_id = ?
                """, userId);
    }

    public boolean isPushEnabled(UUID userId, NotificationType type) {
        return findByUserIdAndNotificationType(userId, type)
                .map(p -> Boolean.TRUE.equals(p.getPushEnabled())).orElse(true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(User user) {
        Object value = user.getSettings() == null ? null : user.getSettings().get(KEY);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Optional<NotificationPreference> toPreference(
            UUID userId, String type, Map<?, ?> channels) {
        try {
            return Optional.of(NotificationPreference.builder()
                    .preferenceId(UUID.nameUUIDFromBytes((userId + ":" + type).getBytes()))
                    .userId(userId)
                    .notificationType(NotificationType.valueOf(type))
                    .pushEnabled(flag(channels, "pushEnabled"))
                    .emailEnabled(flag(channels, "emailEnabled"))
                    .inAppEnabled(flag(channels, "inAppEnabled"))
                    .build());
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private boolean flag(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null || Boolean.parseBoolean(value.toString());
    }
}
