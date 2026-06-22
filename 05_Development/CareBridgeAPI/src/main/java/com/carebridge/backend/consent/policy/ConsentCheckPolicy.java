package com.carebridge.backend.consent.policy;

import com.carebridge.backend.common.exception.ConsentException;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ConsentCheckPolicy {

    public void ensureGranted(boolean granted, Long userId, ConsentDataType dataType, ConsentPurpose purpose) {
        if (!granted) {
            throw new ConsentException("Consent denied for user=%s, dataType=%s, purpose=%s"
                    .formatted(userId, dataType, purpose));
        }
    }

    public boolean isValid(Instant expiryAt, Instant revokedAt) {
        return revokedAt == null && expiryAt != null && expiryAt.isAfter(Instant.now());
    }
}
