package com.carebridge.backend.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for PUT /api/v1/users/me/notification-preferences (UC-10).
 */
public record UpdateNotificationPreferencesRequest(
    @NotEmpty @Valid List<NotificationPreferenceItemDto> preferences
) {}
