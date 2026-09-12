package com.carebridge.backend.notification.dto;

import com.carebridge.backend.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
    @NotNull UUID recipientUserId,
    @NotNull NotificationType type,
    @NotBlank @Size(max = 255) String title,
    @NotBlank String body,
    UUID referenceId,
    @Size(max = 50) String referenceType,
    Map<String, String> metadata
) {
    public SendNotificationRequest(
        UUID recipientUserId,
        NotificationType type,
        String title,
        String body,
        UUID referenceId,
        String referenceType
    ) {
        this(recipientUserId, type, title, body, referenceId, referenceType, null);
    }
}
