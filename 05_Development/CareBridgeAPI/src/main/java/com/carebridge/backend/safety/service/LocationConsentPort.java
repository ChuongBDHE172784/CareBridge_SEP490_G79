package com.carebridge.backend.safety.service;

import java.util.UUID;

public interface LocationConsentPort {
    boolean hasLocationConsent(UUID userId);
}
