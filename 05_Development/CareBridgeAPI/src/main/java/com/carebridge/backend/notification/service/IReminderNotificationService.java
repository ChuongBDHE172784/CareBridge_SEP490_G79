package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.ReminderNotificationCommand;
import java.util.UUID;

public interface IReminderNotificationService {

    NotificationRecordResponse sendReminderNotification(UUID reminderId, UUID userId, String title, String body);

    NotificationRecordResponse sendAppointmentNotification(ReminderNotificationCommand command);
}
