package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.NotificationPreference;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Persists notification preferences inside the canonical users.settings_jsonb document. */
@Repository
@RequiredArgsConstructor
public class NotificationPreferenceRepository {
    private static final String KEY = "notifications";
    private final UserRepository userRepository;

    public List<NotificationPreference> findByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return List.of();
        Map<String, Object> values = section(user);
        List<NotificationPreference> result = new ArrayList<>();
        values.forEach((type, value) -> {
            if (value instanceof Map<?, ?> channels) result.add(toPreference(userId, type, channels));
        });
        return result;
    }

    public Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, NotificationType type) {
        return findByUserId(userId).stream().filter(p -> p.getNotificationType() == type).findFirst();
    }

    public NotificationPreference save(NotificationPreference preference) {
        User user = userRepository.findById(preference.getUserId()).orElseThrow();
        Map<String, Object> settings = mutableSettings(user);
        Map<String, Object> notifications = mutableSection(settings);
        notifications.put(preference.getNotificationType().name(), Map.of(
                "pushEnabled", Boolean.TRUE.equals(preference.getPushEnabled()),
                "emailEnabled", Boolean.TRUE.equals(preference.getEmailEnabled()),
                "inAppEnabled", Boolean.TRUE.equals(preference.getInAppEnabled())));
        settings.put(KEY, notifications);
        user.setSettings(settings);
        userRepository.save(user);
        if (preference.getPreferenceId() == null) preference.setPreferenceId(UUID.randomUUID());
        if (preference.getCreatedAt() == null) preference.setCreatedAt(Instant.now());
        preference.setUpdatedAt(Instant.now());
        return preference;
    }

    public void deleteByUserId(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            Map<String, Object> settings = mutableSettings(user);
            settings.remove(KEY);
            user.setSettings(settings);
            userRepository.save(user);
        });
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

    private Map<String, Object> mutableSettings(User user) {
        return user.getSettings() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(user.getSettings());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mutableSection(Map<String, Object> settings) {
        Object value = settings.get(KEY);
        return value instanceof Map<?, ?> map
                ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
    }

    private NotificationPreference toPreference(UUID userId, String type, Map<?, ?> channels) {
        return NotificationPreference.builder().preferenceId(UUID.nameUUIDFromBytes((userId + ":" + type).getBytes()))
                .userId(userId).notificationType(NotificationType.valueOf(type))
                .pushEnabled(flag(channels, "pushEnabled")).emailEnabled(flag(channels, "emailEnabled"))
                .inAppEnabled(flag(channels, "inAppEnabled")).build();
    }

    private boolean flag(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null || Boolean.parseBoolean(value.toString());
    }
}
