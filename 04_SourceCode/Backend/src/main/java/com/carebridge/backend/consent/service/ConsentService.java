package com.carebridge.backend.consent.service;

import com.carebridge.backend.consent.dto.request.GrantConsentRequest;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.util.List;
import java.util.UUID;

public interface ConsentService {

    ConsentGrantResponse grantConsent(UUID userId, GrantConsentRequest request);

    ConsentGrantResponse revokeConsent(UUID userId, Long consentId);

    List<ConsentGrantResponse> listConsents(UUID userId);

    void ensureConsent(UUID userId, ConsentDataType dataType, ConsentPurpose purpose);
}
