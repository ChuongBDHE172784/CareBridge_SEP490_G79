package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import java.time.Instant;
import java.util.UUID;

public record UpdatePartnerProfileResponse(
        UUID id,
        String name,
        OrganizationType type,
        String address,
        String city,
        String phone,
        String email,
        String website,
        String logoUrl,
        String description,
        OrganizationStatus status,
        Instant updatedAt) {}
