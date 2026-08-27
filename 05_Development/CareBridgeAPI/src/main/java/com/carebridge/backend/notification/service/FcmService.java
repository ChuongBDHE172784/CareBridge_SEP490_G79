package com.carebridge.backend.notification.service;

import java.util.List;
import java.util.Map;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;

public interface FcmService {

    /**
     * Whether this implementation is backed by an initialized delivery provider.
     * A worker must not claim durable reminder jobs while this capability is false.
     * Test doubles default to ready so they can exercise scheduling logic without
     * coupling unit tests to Firebase configuration.
     */
    default boolean isReady() {
        return true;
    }

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

    FcmDeliveryResult sendWithRetry(String fcmToken, String title, String body, int maxAttempts);

    FcmDeliveryResult sendWithRetry(
            String fcmToken,
            String title,
            String body,
            Map<String, String> data,
            int maxAttempts);
}
