package com.carebridge.backend.notification.service;

import java.util.List;

public interface FcmService {

    /**
     * Send a push notification to a single FCM token.
     * Returns the FCM message ID on success, or null if FCM is disabled/unavailable.
     */
    String sendToToken(String fcmToken, String title, String body);

    /**
     * Send a push notification to multiple FCM tokens (multicast).
     * Returns count of successful deliveries.
     */
    int sendToTokens(List<String> fcmTokens, String title, String body);
}
