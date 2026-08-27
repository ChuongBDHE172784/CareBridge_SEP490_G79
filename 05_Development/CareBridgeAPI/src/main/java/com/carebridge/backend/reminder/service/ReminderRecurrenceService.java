package com.carebridge.backend.reminder.service;

import com.carebridge.backend.reminder.entity.RecurrenceType;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class ReminderRecurrenceService {

    public Optional<GeneratedOccurrence> occurrenceForDate(Reminder reminder, LocalDate targetDate, ZoneId timezone) {
        RecurrenceType recurrenceType = reminder.getRecurrenceType() == null
                ? RecurrenceType.NONE
                : reminder.getRecurrenceType();

        if (recurrenceType == RecurrenceType.NONE) {
            return nonRecurringOccurrenceForDate(reminder, targetDate, timezone);
        }

        LocalDate scheduledDate = LocalDateTime.ofInstant(reminder.getScheduledAt(), timezone).toLocalDate();
        LocalDate endDate = reminder.getRecurrenceEndDate() == null
                ? null
                : LocalDateTime.ofInstant(reminder.getRecurrenceEndDate(), timezone).toLocalDate();

        if (targetDate.isBefore(scheduledDate) || (endDate != null && targetDate.isAfter(endDate))) {
            return Optional.empty();
        }

        boolean occurs = switch (recurrenceType) {
            case DAILY -> true;
            case WEEKLY -> targetDate.getDayOfWeek() == scheduledDate.getDayOfWeek();
            case MONTHLY -> targetDate.getDayOfMonth() == scheduledDate.getDayOfMonth();
            case NONE -> false;
        };

        if (!occurs) {
            return Optional.empty();
        }

        LocalDateTime scheduledLocal = LocalDateTime.ofInstant(reminder.getScheduledAt(), timezone);
        Instant occurrenceDueAt = targetDate
                .atTime(scheduledLocal.toLocalTime())
                .atZone(timezone)
                .toInstant();

        ReminderStatus occurrenceStatus = recurringStatusForDate(reminder, targetDate, timezone);
        Instant occurrenceDue = occurrenceStatus == ReminderStatus.SNOOZED && reminder.getSnoozedUntil() != null
                ? reminder.getSnoozedUntil()
                : occurrenceDueAt;
        Instant occurrenceSnoozedUntil = occurrenceStatus == ReminderStatus.SNOOZED
                ? reminder.getSnoozedUntil()
                : null;

        return Optional.of(new GeneratedOccurrence(
                reminder,
                occurrenceDueAt,
                occurrenceDue,
                occurrenceStatus,
                occurrenceSnoozedUntil));
    }

    private Optional<GeneratedOccurrence> nonRecurringOccurrenceForDate(
            Reminder reminder,
            LocalDate targetDate,
            ZoneId timezone) {
        ReminderStatus status = reminder.getStatus();
        if (status != ReminderStatus.PENDING
                && status != ReminderStatus.SNOOZED
                && status != ReminderStatus.COMPLETED) {
            return Optional.empty();
        }

        Instant dueAt = status == ReminderStatus.SNOOZED && reminder.getSnoozedUntil() != null
                ? reminder.getSnoozedUntil()
                : reminder.getScheduledAt();
        LocalDate dueDate = LocalDateTime.ofInstant(dueAt, timezone).toLocalDate();

        if (!dueDate.equals(targetDate)) {
            return Optional.empty();
        }

        return Optional.of(new GeneratedOccurrence(
                reminder,
                reminder.getScheduledAt(),
                dueAt,
                status,
                reminder.getSnoozedUntil()));
    }

    private ReminderStatus recurringStatusForDate(Reminder reminder, LocalDate targetDate, ZoneId timezone) {
        ReminderStatus status = reminder.getStatus();
        if (isOccurrenceActionFromTargetDate(reminder, targetDate, timezone)) {
            return status;
        }
        return resetRecurringStatus(status);
    }

    private boolean isOccurrenceActionFromTargetDate(Reminder reminder, LocalDate targetDate, ZoneId timezone) {
        ReminderStatus status = reminder.getStatus();
        if (status != ReminderStatus.COMPLETED
                && status != ReminderStatus.SKIPPED
                && status != ReminderStatus.SNOOZED) {
            return false;
        }
        if (reminder.getUpdatedAt() == null) {
            return false;
        }
        LocalDate updatedDate = LocalDateTime.ofInstant(reminder.getUpdatedAt(), timezone).toLocalDate();
        return updatedDate.equals(targetDate);
    }

    private ReminderStatus resetRecurringStatus(ReminderStatus status) {
        if (status == ReminderStatus.COMPLETED
                || status == ReminderStatus.SKIPPED
                || status == ReminderStatus.SNOOZED) {
            return ReminderStatus.PENDING;
        }
        return status;
    }

    public record GeneratedOccurrence(
            Reminder reminder,
            Instant scheduledAt,
            Instant dueAt,
            ReminderStatus status,
            Instant snoozedUntil) {
    }
}
