package com.carebridge.backend.reminder.schedule.dto;

import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleRecurrence;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReminderScheduleResponse(
        UUID id,
        String title,
        List<String> times,
        String timeZone,
        ReminderScheduleRecurrence recurrence,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        long revision) {
}
