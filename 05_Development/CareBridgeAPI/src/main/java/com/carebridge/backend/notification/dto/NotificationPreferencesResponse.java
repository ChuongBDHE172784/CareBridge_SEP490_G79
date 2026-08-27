package com.carebridge.backend.notification.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response for GET/PUT /api/v1/users/me/notification-preferences (UC-10).
 */
public record NotificationPreferencesResponse(
    UUID userId,
    List<NotificationPreferenceItemDto> preferences,
    List<Integer> appointmentReminderDefaults
) {}
