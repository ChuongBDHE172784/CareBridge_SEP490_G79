package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.service.FcmService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Stub FcmService used when FCM is disabled or not yet configured.
 * Replace with firebase-admin SDK implementation once credentials are in place.
 */
@Service
@ConditionalOnProperty(name = "carebridge.fcm.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class FcmServiceImpl implements FcmService {

    @Override
    public String sendToToken(String fcmToken, String title, String body) {
        log.info("[FCM-STUB] Would send to token={} title='{}' body='{}'",
                fcmToken.substring(0, Math.min(fcmToken.length(), 12)) + "...", title, body);
        return null;
    }

    @Override
    public int sendToTokens(List<String> fcmTokens, String title, String body) {
        log.info("[FCM-STUB] Would send to {} token(s) title='{}' body='{}'", fcmTokens.size(), title, body);
        return 0;
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
        return sendWithRetry(fcmToken, title, body, maxAttempts);
    }
}
