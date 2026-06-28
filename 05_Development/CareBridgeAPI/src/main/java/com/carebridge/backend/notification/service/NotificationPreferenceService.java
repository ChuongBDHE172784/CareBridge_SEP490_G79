package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationPreferencesResponse;
import com.carebridge.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.carebridge.backend.notification.entity.NotificationType;

import java.util.UUID;

/**
 * UC-10: Update and retrieve notification preferences.
 * UC-158/159/160/161: Preference gate — check if a notification type is enabled.
 */
public interface NotificationPreferenceService {

    /**
     * UC-10: Get all notification preferences for the current user.
     *
     * @param userId user ID extracted from JWT
     * @return current preferences (defaults to all enabled when no row exists)
     */
    NotificationPreferencesResponse getPreferences(UUID userId);

    /**
     * UC-10: Upsert notification preferences for the current user.
     * Idempotent — calling twice with the same data produces the same result.
     *
     * @param userId  user ID extracted from JWT (never from request body)
     * @param request list of preference items to update
     * @return updated preferences
     */
    NotificationPreferencesResponse updatePreferences(UUID userId, UpdateNotificationPreferencesRequest request);

    /**
     * UC-158/159/160/161: Check whether push notifications are enabled for a type.
     * Returns {@code true} when no preference row exists (opt-out model: default = enabled).
     *
     * @param userId user ID
     * @param type   notification type to check
     * @return true if push notifications should be delivered for this type
     */
    boolean isPushEnabled(UUID userId, NotificationType type);
}
