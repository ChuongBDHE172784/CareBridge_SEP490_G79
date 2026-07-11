package com.carebridge.backend.partner;

import com.carebridge.backend.partner.dto.request.SubmitServiceListingRequest;
import com.carebridge.backend.partner.entity.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class SubmitServiceListingTestFactory {
    static final UUID OWNER_ID = UUID.fromString("f3000000-0000-0000-0000-000000000001");
    static final UUID ORG_ID = UUID.fromString("f4000000-0000-0000-0000-000000000001");
    static final UUID SERVICE_ID = UUID.fromString("f5000000-0000-0000-0000-000000000001");
    private SubmitServiceListingTestFactory() {}
    static PartnerOrganization org(OrganizationStatus status) {
        return PartnerOrganization.builder().id(ORG_ID).name("ABC").type(OrganizationType.CLINIC)
                .address("A").city("Hanoi").phone("0901234567").email("a@abc.vn")
                .status(status).representativeUserId(OWNER_ID).build();
    }
    static SubmitServiceListingRequest request(String currency) {
        return new SubmitServiceListingRequest("Antenatal care", "Package", new BigDecimal("500000"), currency, "https://abc.vn/book");
    }
    static PartnerService saved() {
        return PartnerService.builder().id(SERVICE_ID).partnerId(ORG_ID).serviceName("Antenatal care")
                .description("Package").priceFrom(new BigDecimal("500000")).currency("VND")
                .bookingUrl("https://abc.vn/book").approvalStatus(ServiceApprovalStatus.PENDING)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
}
