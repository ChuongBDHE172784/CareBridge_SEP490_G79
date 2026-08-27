package com.carebridge.backend.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record ReminderNotificationCommand(
        UUID jobId,
        UUID reminderId,
        UUID occurrenceId,
        UUID userId,
        String appointmentTitle,
        Instant occurrenceScheduledAt,
        int offsetMinutes,
        String timeZone) {
}
