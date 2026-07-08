package com.carebridge.backend.reminder.dto;

import com.carebridge.backend.reminder.entity.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateVaccinationReminderRequest {

    @NotNull
    private UUID babyId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotNull
    private Instant scheduledAt;

    private RecurrenceType recurrenceType;

    private Instant recurrenceEndDate;

    private UUID journeyId;
}
