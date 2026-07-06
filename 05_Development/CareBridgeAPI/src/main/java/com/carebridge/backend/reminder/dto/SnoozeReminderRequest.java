package com.carebridge.backend.reminder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class SnoozeReminderRequest {

    @NotNull
    private Instant snoozedUntil;
}
