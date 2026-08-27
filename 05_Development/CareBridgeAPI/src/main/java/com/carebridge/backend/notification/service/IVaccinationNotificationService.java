package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.VaccinationReminderCommand;

public interface IVaccinationNotificationService {

    /**
     * Delivers one vaccination reminder push, reusing the shared preferences, device-token,
     * FCM and notification-record path.
     *
     * @return the notification record, or {@code null} when the mother has push disabled for
     *         reminders or the milestone has already been delivered
     */
    NotificationRecordResponse sendVaccinationReminder(VaccinationReminderCommand command);
}
