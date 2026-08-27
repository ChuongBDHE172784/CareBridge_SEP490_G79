package com.carebridge.backend.reminder.dto;

import com.carebridge.backend.reminder.entity.RecurrenceType;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UpdateReminderRequest {

    @Size(max = 255)
    private String title;

    private Instant scheduledAt;

    private RecurrenceType recurrenceType;

    private Instant recurrenceEndDate;

    private Boolean recurrenceEndDateSet;

    private List<Integer> notificationOffsetsMinutes;

    private Boolean notificationOffsetsMinutesSet;

    private String timeZone;
}
