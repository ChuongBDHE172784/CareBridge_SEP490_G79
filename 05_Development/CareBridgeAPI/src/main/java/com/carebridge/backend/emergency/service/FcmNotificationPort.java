package com.carebridge.backend.emergency.service;

import java.util.List;
import java.util.Map;

public interface FcmNotificationPort {
    void sendBatch(List<String> fcmTokens, Map<String, String> payload);
}
