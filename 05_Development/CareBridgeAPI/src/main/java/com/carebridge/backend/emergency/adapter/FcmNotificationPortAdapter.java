package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Delegates to the existing {@link FcmService}. Note: FcmService itself is a
 * logging stub until carebridge.fcm.enabled=true and a real Firebase Admin
 * SDK implementation are configured — wiring here makes the consent/roster
 * logic real, but no push is actually delivered until that ops step happens.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FcmNotificationPortAdapter implements FcmNotificationPort {

    private final FcmService fcmService;

    @Override
    public void sendBatch(List<String> fcmTokens, Map<String, String> payload) {
        if (fcmTokens.isEmpty()) {
            return;
        }
        String title = "Cảnh báo khẩn cấp từ CareBridge";
        String body = "MANUAL".equals(payload.get("triggerSource"))
                ? "Người thân của bạn vừa kích hoạt chế độ khẩn cấp. Vui lòng kiểm tra ngay."
                : "Hệ thống phát hiện dấu hiệu khẩn cấp cho người thân của bạn. Vui lòng kiểm tra ngay.";
        int sent = fcmService.sendToTokens(fcmTokens, title, body);
        log.info("FCM batch dispatched to {} token(s), {} reported sent", fcmTokens.size(), sent);
    }
}
