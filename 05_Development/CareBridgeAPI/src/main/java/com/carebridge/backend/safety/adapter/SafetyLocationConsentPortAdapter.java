package com.carebridge.backend.safety.adapter;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.safety.service.LocationConsentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SafetyLocationConsentPortAdapter implements LocationConsentPort {

    private final ConsentGrantRepository consentGrantRepository;

    @Override
    public boolean hasLocationConsent(UUID userId) {
        return consentGrantRepository.existsValidConsent(
                userId, ConsentDataType.LOCATION, ConsentPurpose.SHARE, Instant.now());
    }
}
