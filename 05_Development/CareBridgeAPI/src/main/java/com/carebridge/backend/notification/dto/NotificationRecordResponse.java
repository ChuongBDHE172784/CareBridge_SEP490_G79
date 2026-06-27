package com.carebridge.backend.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecordResponse(
    UUID id,
    UUID userId,
    String type,
    String title,
    String body,
    UUID referenceId,
    String referenceType,
    String status,
    Instant createdAt,
    Instant sentAt
) {}
