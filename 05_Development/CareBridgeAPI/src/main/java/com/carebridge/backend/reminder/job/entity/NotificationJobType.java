package com.carebridge.backend.reminder.job.entity;

/**
 * Discriminator for {@code notification_jobs}.
 *
 * <p>Every query against the consolidated queue must filter on this: two workers
 * share one table, and a claim that ignores the type would let the appointment
 * worker pick up a reminder-schedule job it cannot interpret.
 */
public enum NotificationJobType {
    REMINDER_SCHEDULE,
    APPOINTMENT
}
