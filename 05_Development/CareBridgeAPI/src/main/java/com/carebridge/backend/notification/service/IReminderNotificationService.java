package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import java.util.UUID;

public interface IReminderNotificationService {

    NotificationRecordResponse sendReminderNotification(UUID reminderId, UUID userId, String title, String body);
}
