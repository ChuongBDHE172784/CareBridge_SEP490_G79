package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.FcmNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FcmNotificationPortAdapter implements FcmNotificationPort {

    @Override
    public void sendBatch(List<String> fcmTokens, Map<String, String> payload) {
        log.warn("FcmNotificationPort not implemented — skipping FCM batch send to {} tokens", fcmTokens.size());
    }
}
