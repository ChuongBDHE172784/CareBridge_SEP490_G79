package com.carebridge.backend.notification.dto;

import com.carebridge.backend.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for a single notification preference item (UC-10).
 * Channel enablement is represented by three boolean flags matching the V1 DB schema.
 */
public record NotificationPreferenceItemDto(
    @NotNull NotificationType notificationType,
    Boolean pushEnabled,
    Boolean emailEnabled,
    Boolean inAppEnabled
) {}
