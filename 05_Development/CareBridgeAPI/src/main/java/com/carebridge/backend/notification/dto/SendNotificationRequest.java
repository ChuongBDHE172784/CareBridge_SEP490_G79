package com.carebridge.backend.notification.dto;

import com.carebridge.backend.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendNotificationRequest(
    @NotNull UUID recipientUserId,
    @NotNull NotificationType type,
    @NotBlank @Size(max = 255) String title,
    @NotBlank String body,
    UUID referenceId,
    @Size(max = 50) String referenceType
) {}
