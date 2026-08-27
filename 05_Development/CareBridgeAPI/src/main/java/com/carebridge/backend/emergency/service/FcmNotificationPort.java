package com.carebridge.backend.emergency.service;

import java.util.List;
import java.util.Map;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;

public interface FcmNotificationPort {
    void sendBatch(List<String> fcmTokens, Map<String, String> payload);

    FcmDeliveryResult send(String fcmToken, Map<String, String> payload);
}
