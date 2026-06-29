package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.LocationConsentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class EmergencyLocationConsentPortAdapter implements LocationConsentPort {

    @Override
    public boolean hasLocationConsent(UUID userId) {
        log.warn("Emergency LocationConsentPort not implemented — defaulting to false for user {}", userId);
        return false;
    }
}
