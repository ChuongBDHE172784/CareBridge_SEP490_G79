package com.carebridge.backend.notification.dto;

import java.util.Map;
import java.util.UUID;

public record ConsultationNotificationPayload(
        UUID consultationId,
        UUID motherUserId,
        UUID expertUserId,
        ConsultationNotificationEventType eventType,
        String title,
        String body,
        Map<String, String> data
) {
}
