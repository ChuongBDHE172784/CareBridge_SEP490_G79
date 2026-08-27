package com.carebridge.backend.consent.service;

import com.carebridge.backend.consent.dto.request.GrantConsentRequest;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.util.List;

public interface ConsentService {

    ConsentGrantResponse grantConsent(java.util.UUID userId, GrantConsentRequest request);

    ConsentGrantResponse revokeConsent(java.util.UUID userId, Long consentId);

    List<ConsentGrantResponse> listConsents(java.util.UUID userId);

    void ensureConsent(java.util.UUID userId, ConsentDataType dataType, ConsentPurpose purpose);
}
