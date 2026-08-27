package com.carebridge.backend.reminder.appointment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-only appointment projection for an explicitly authorised care group. */
public record SharedAppointmentResponse(
        UUID id,
        UUID careGroupId,
        String reminderType,
        String title,
        Instant scheduledAt,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<Integer> notificationOffsetsMinutes,
        String timeZone) {
}
