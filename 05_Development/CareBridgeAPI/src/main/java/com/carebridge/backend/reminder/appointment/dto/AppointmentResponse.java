package com.carebridge.backend.reminder.appointment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Appointment-only wire model; recurrence and task lifecycle fields are omitted. */
public record AppointmentResponse(
        UUID id,
        String reminderType,
        String title,
        Instant scheduledAt,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<Integer> notificationOffsetsMinutes,
        String timeZone) {
}
