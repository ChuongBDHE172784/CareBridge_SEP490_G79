package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.service.FcmService;
import java.util.List;
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
        // Never write device tokens or health-related notification content to logs.
        log.info("[FCM-STUB] delivery suppressed tokenPresent={} titleLength={} bodyLength={}",
                fcmToken != null && !fcmToken.isBlank(),
                title == null ? 0 : title.length(),
                body == null ? 0 : body.length());
        return null;
    }

    @Override
    public int sendToTokens(List<String> fcmTokens, String title, String body) {
        // Keep fallback logs metadata-only; payloads may contain sensitive care data.
        log.info("[FCM-STUB] delivery suppressed tokenCount={} titleLength={} bodyLength={}",
                fcmTokens == null ? 0 : fcmTokens.size(),
                title == null ? 0 : title.length(),
                body == null ? 0 : body.length());
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
}
