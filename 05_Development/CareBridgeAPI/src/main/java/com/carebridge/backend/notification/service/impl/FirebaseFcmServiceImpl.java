package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Real FcmService backed by the Firebase Admin SDK.
 * Active only when carebridge.fcm.enabled=true (see {@link FcmServiceImpl}
 * for the stub used otherwise).
 */
@Service
@ConditionalOnProperty(name = "carebridge.fcm.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class FirebaseFcmServiceImpl implements FcmService {

    private final FirebaseApp firebaseApp;

    @Override
    public boolean isReady() {
        return firebaseApp != null;
    }

    @Override
    public String sendToToken(String fcmToken, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .build();
            return FirebaseMessaging.getInstance(firebaseApp).send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed for token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public int sendToTokens(List<String> fcmTokens, String title, String body) {
        if (fcmTokens.isEmpty()) {
            return 0;
        }
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(fcmTokens)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .build();
            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                log.warn("FCM multicast: {} succeeded, {} failed", response.getSuccessCount(), response.getFailureCount());
            }
            return response.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            log.warn("FCM multicast send failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public FcmDeliveryResult sendWithRetry(String fcmToken, String title, String body, int maxAttempts) {
        int attempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            String messageId = sendToToken(fcmToken, title, body);
            if (messageId != null && !messageId.isBlank()) {
                return FcmDeliveryResult.success(messageId, attempt);
            }
        }
        return FcmDeliveryResult.failed("FCM_SEND_FAILED", attempts);
    }

    @Override
    public FcmDeliveryResult sendWithRetry(
            String fcmToken,
            String title,
            String body,
            Map<String, String> data,
            int maxAttempts) {
        int attempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Message message = buildDataMessage(fcmToken, title, body, data);
                String messageId = FirebaseMessaging.getInstance(firebaseApp).send(message);
                if (messageId != null && !messageId.isBlank()) {
                    return FcmDeliveryResult.success(messageId, attempt);
                }
            } catch (FirebaseMessagingException e) {
                log.warn("FCM data send failed: {}", e.getMessage());
            }
        }
        return FcmDeliveryResult.failed("FCM_SEND_FAILED", attempts);
    }

    static Message buildDataMessage(
            String fcmToken, String title, String body, Map<String, String> data) {
        return Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .build();
    }
}
