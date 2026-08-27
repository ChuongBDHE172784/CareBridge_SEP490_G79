package com.carebridge.backend.reminder.schedule.dto;

import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleRecurrence;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdateReminderScheduleRequest(
        @Size(max = 255) String title,
        @Size(max = 96)
        List<@Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$") String> times,
        @Size(max = 80) String timeZone,
        ReminderScheduleRecurrence recurrence,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active,
        /** Set true when the caller intentionally clears an existing end date. */
        Boolean endDateSet) {
}
