package com.carebridge.backend.safety.policy;

import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.consent.service.ConsentService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SafetyConsentPolicy {

    private final ConsentService consentService;
    private final ConsentGrantRepository consentGrantRepository;

    public void requireSensorCollection(UUID userId) {
        consentService.ensureConsent(userId, ConsentDataType.SENSOR_DATA, ConsentPurpose.CREATE);
    }

    public boolean mayPersistLocation(UUID userId) {
        return consentGrantRepository.existsValidConsent(
                userId, ConsentDataType.LOCATION, ConsentPurpose.SHARE, Instant.now());
    }
}
