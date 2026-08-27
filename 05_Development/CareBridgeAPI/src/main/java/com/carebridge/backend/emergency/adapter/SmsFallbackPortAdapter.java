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
        log.error("SMS fallback delivery outcome=PROVIDER_NOT_CONFIGURED");
        throw new IllegalStateException("SMS fallback provider is not configured");
    }
}
