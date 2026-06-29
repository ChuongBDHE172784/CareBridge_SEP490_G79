package com.carebridge.backend.reminder.service;

import java.time.Instant;
import java.util.UUID;

public interface INotificationService {

    /**
     * Schedule a push notification via FCM.
     * @return FCM job ID for later cancellation
     */
    String scheduleFcmPush(UUID userId, String title, String body, Instant scheduledAt);
}
