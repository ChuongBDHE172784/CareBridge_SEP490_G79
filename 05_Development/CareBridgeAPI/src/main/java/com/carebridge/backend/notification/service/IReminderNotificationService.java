package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.ReminderNotificationCommand;
import java.util.UUID;

public interface IReminderNotificationService {

    NotificationRecordResponse sendReminderNotification(UUID reminderId, UUID userId, String title, String body);

    /** Alarm-schedule delivery keeps a distinct call site while reusing the
     * existing preferences, device-token, FCM and notification-record path. */
    default NotificationRecordResponse sendReminderScheduleNotification(
            UUID scheduleId, UUID userId, String title, String body) {
        return sendReminderNotification(scheduleId, userId, title, body);
    }

    /** Job-aware overload used by durable schedule workers. Implementations may
     * use jobId as the natural idempotency key for notification records. */
    default NotificationRecordResponse sendReminderScheduleNotification(
            UUID scheduleId, UUID jobId, UUID userId, String title, String body,
            java.time.LocalDate occurrenceDate, java.time.LocalTime localTime, String timeZone) {
        return sendReminderScheduleNotification(scheduleId, userId, title, body);
    }

    NotificationRecordResponse sendAppointmentNotification(ReminderNotificationCommand command);
}
