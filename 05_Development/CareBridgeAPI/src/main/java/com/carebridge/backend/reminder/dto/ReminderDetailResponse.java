package com.carebridge.backend.reminder.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReminderDetailResponse {

    private UUID id;
    private String reminderType;
    private String title;
    private Instant scheduledAt;
    private String recurrenceType;
    private String status;
    private Instant snoozedUntil;
    private Instant createdAt;
    private Instant updatedAt;
}
