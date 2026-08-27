package com.carebridge.backend.emergency.service;

import java.util.UUID;

public interface LocationConsentPort {
    boolean hasLocationConsent(UUID userId);
}
