package com.carebridge.backend.partner;

import com.carebridge.backend.partner.dto.request.UpdatePartnerProfileRequest;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.OrganizationType;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import java.time.Instant;
import java.util.UUID;

final class UpdatePartnerProfileTestFactory {
    static final UUID OWNER_ID = UUID.fromString("f1000000-0000-0000-0000-000000000001");
    static final UUID PARTNER_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    private UpdatePartnerProfileTestFactory() {}

    static PartnerOrganization partner(OrganizationStatus status) {
        return PartnerOrganization.builder().id(PARTNER_ID).name("Old Clinic")
                .type(OrganizationType.CLINIC).address("Old address").city("Hanoi")
                .phone("0901234567").email("old@clinic.vn").description("Old")
                .status(status).representativeUserId(OWNER_ID)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    static UpdatePartnerProfileRequest request() {
        return UpdatePartnerProfileRequest.builder().name("Updated Clinic")
                .type(OrganizationType.CLINIC).address("New address").city("Hanoi")
                .phone("84 90 765 4321").email("new@clinic.vn").website("https://clinic.vn")
                .logoUrl("https://clinic.vn/logo.png").description("Updated").build();
    }
}
