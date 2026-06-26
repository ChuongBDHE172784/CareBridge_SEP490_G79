package com.carebridge.backend.partner.mapper;

import com.carebridge.backend.partner.dto.request.CreatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import org.springframework.stereotype.Component;

@Component
public class PartnerProfileMapper {

    // Oracle: ADR-003 — status MUST be PENDING_APPROVAL; ADR-002 — actorId from SecurityContext
    public PartnerOrganization toEntity(CreatePartnerProfileRequest request, java.util.UUID actorId) {
        return PartnerOrganization.builder()
                .name(request.getName())
                .type(request.getType())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .description(request.getDescription())
                .status(OrganizationStatus.PENDING_APPROVAL)
                .representativeUserId(actorId)
                .build();
    }

    public CreatePartnerProfileResponse toResponse(PartnerOrganization entity) {
        return CreatePartnerProfileResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
