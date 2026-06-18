package com.carebridge.backend.consent.service;

import com.carebridge.backend.consent.dto.request.GrantConsentRequest;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import java.util.List;

public interface ConsentService {

    ConsentGrantResponse grantConsent(Long userId, GrantConsentRequest request);

    ConsentGrantResponse revokeConsent(Long userId, Long consentId);

    List<ConsentGrantResponse> listConsents(Long userId);

    void ensureConsent(Long userId, ConsentDataType dataType, ConsentPurpose purpose);
}
