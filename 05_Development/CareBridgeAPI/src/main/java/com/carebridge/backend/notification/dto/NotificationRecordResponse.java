package com.carebridge.backend.notification.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

// ADR-MEDI-004 mục 9 — metadata added (additive) so the mobile client can deep-link a MESSAGE
// notification to its conversationId without a second API call.
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
    Instant sentAt,
    boolean isRead,
    Instant readAt,
    String channel,
    String fcmMessageId,
    int attemptCount,
    Instant failedAt,
    Instant updatedAt,
    Map<String, String> metadata
) {}
