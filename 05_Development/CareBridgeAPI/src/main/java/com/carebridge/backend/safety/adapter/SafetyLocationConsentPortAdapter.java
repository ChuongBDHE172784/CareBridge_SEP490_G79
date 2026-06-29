package com.carebridge.backend.safety.adapter;

import com.carebridge.backend.safety.service.LocationConsentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SafetyLocationConsentPortAdapter implements LocationConsentPort {

    @Override
    public boolean hasLocationConsent(UUID userId) {
        log.warn("Safety LocationConsentPort not implemented — defaulting to false for user {}", userId);
        return false;
    }
}
