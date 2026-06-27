package com.carebridge.backend.notification.service.impl;

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
        log.info("[FCM-STUB] Would send to token={} title='{}' body='{}'",
                fcmToken.substring(0, Math.min(fcmToken.length(), 12)) + "...", title, body);
        return null;
    }

    @Override
    public int sendToTokens(List<String> fcmTokens, String title, String body) {
        log.info("[FCM-STUB] Would send to {} token(s) title='{}' body='{}'", fcmTokens.size(), title, body);
        return 0;
    }
}
