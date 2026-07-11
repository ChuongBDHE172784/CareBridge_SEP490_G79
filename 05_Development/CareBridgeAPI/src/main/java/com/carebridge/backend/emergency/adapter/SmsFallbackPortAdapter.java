package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.SmsFallbackPort;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsFallbackPortAdapter implements SmsFallbackPort {

    @Override
    public void sendFallback(UUID userId, UUID sessionId, String message) {
        log.warn("SMS fallback placeholder triggered for emergency session [{}], user [{}]", sessionId, userId);
    }
}
