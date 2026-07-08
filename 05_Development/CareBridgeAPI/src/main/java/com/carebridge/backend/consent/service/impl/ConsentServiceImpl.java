package com.carebridge.backend.consent.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.constants.ConsentConstants;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.common.util.StringUtils;
import com.carebridge.backend.consent.dto.request.GrantConsentRequest;
import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentDataType;
import com.carebridge.backend.consent.entity.ConsentGrant;
import com.carebridge.backend.consent.entity.ConsentPurpose;
import com.carebridge.backend.consent.mapper.ConsentGrantMapper;
import com.carebridge.backend.consent.policy.ConsentCheckPolicy;
import com.carebridge.backend.consent.repository.ConsentGrantRepository;
import com.carebridge.backend.consent.service.ConsentService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsentServiceImpl implements ConsentService {

    private final ConsentGrantRepository consentGrantRepository;
    private final ConsentGrantMapper consentGrantMapper;
    private final ConsentCheckPolicy consentCheckPolicy;
    private final AuditService auditService;

    @Override
    public ConsentGrantResponse grantConsent(java.util.UUID userId, GrantConsentRequest request) {
        Instant now = Instant.now();
        int expiryDays = request.getExpiryDays() == null
                ? ConsentConstants.DEFAULT_EXPIRY_DAYS
                : request.getExpiryDays();
        ConsentGrant grant = ConsentGrant.builder()
                .userId(userId)
                .dataType(request.getDataType())
                .purpose(request.getPurpose())
                .recipient(StringUtils.sanitizeBasicText(request.getRecipient()))
                .scope(StringUtils.sanitizeBasicText(request.getScope()))
                .consentGivenAt(now)
                .expiryAt(now.plus(expiryDays, ChronoUnit.DAYS))
                .build();
        ConsentGrant saved = consentGrantRepository.save(grant);
        auditService.log(
                AuditAction.CONSENT_GRANTED,
                userId,
                "ConsentGrant",
                saved.getId().toString(),
                Map.of("dataType", saved.getDataType().name(), "purpose", saved.getPurpose().name()));
        return consentGrantMapper.toResponse(saved);
    }

    @Override
    public ConsentGrantResponse revokeConsent(java.util.UUID userId, Long consentId) {
        ConsentGrant grant = consentGrantRepository.findByIdAndUserId(consentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Consent grant not found"));
        if (grant.getRevokedAt() != null) {
            throw new ValidationException("CONSENT-012: Consent has already been revoked");
        }
        grant.setRevokedAt(Instant.now());
        grant.setRevokedBy(userId);
        ConsentGrant saved = consentGrantRepository.save(grant);
        auditService.log(
                AuditAction.CONSENT_REVOKED,
                userId,
                "ConsentGrant",
                saved.getId().toString(),
                Map.of("dataType", saved.getDataType().name(), "purpose", saved.getPurpose().name()));
        return consentGrantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentGrantResponse> listConsents(java.util.UUID userId) {
        return consentGrantRepository.findByUserIdOrderByConsentGivenAtDesc(userId).stream()
                .map(consentGrantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureConsent(java.util.UUID userId, ConsentDataType dataType, ConsentPurpose purpose) {
        boolean granted = consentGrantRepository.existsValidConsent(userId, dataType, purpose, Instant.now());
        consentCheckPolicy.ensureGranted(granted, userId, dataType, purpose);
    }
}
