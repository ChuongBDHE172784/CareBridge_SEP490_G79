package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import java.time.Instant;
import java.util.UUID;

public record PartnerVerificationQueueItemResponse(UUID id, String name, OrganizationType type,
        OrganizationStatus status, String city, Instant createdAt) {}
