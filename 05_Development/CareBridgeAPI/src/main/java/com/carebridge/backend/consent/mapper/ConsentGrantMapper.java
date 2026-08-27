package com.carebridge.backend.consent.mapper;

import com.carebridge.backend.consent.dto.response.ConsentGrantResponse;
import com.carebridge.backend.consent.entity.ConsentGrant;
import org.springframework.stereotype.Component;

@Component
public class ConsentGrantMapper {

    public ConsentGrantResponse toResponse(ConsentGrant grant) {
        if (grant == null) {
            return null;
        }
        return ConsentGrantResponse.builder()
                .id(grant.getId())
                .permissionId(grant.getPermissionId())
                .userId(grant.getUserId())
                .dataType(grant.getDataType())
                .purpose(grant.getPurpose())
                .recipient(grant.getRecipient())
                .scope(grant.getScope())
                .consentGivenAt(grant.getConsentGivenAt())
                .expiryAt(grant.getExpiryAt())
                .revokedAt(grant.getRevokedAt())
                .status(grant.getStatus())
                .build();
    }
}
