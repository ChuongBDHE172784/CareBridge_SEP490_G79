package com.carebridge.backend.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for PUT /api/v1/users/me/notification-preferences (UC-10).
 */
public record UpdateNotificationPreferencesRequest(
    @NotNull List<@Valid NotificationPreferenceItemDto> preferences,
    List<Integer> appointmentReminderDefaults
) {}
