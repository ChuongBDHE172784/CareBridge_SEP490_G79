package com.carebridge.backend.partner;
import com.carebridge.backend.partner.dto.request.SubmitSponsoredContentRequest;
import com.carebridge.backend.partner.entity.*;
import java.time.*;
import java.util.UUID;
final class SubmitSponsoredContentTestFactory {
 static final UUID OWNER=UUID.fromString("f5000000-0000-0000-0000-000000000001"),ORG=UUID.fromString("f6000000-0000-0000-0000-000000000001"),CAMPAIGN=UUID.fromString("f7000000-0000-0000-0000-000000000001");
 static PartnerOrganization org(OrganizationStatus s){return PartnerOrganization.builder().id(ORG).name("ABC").type(OrganizationType.CLINIC).address("A").city("Hanoi").phone("0901234567").email("a@a.vn").status(s).representativeUserId(OWNER).build();}
 static SubmitSponsoredContentRequest request(){return new SubmitSponsoredContentRequest("Offer","20%",LocalDate.of(2026,7,5),LocalDate.of(2026,7,31),"Sponsored by ABC");}
 static SponsoredCampaign saved(){return SponsoredCampaign.builder().id(CAMPAIGN).partnerId(ORG).title("Offer").description("20%").startDate(LocalDate.of(2026,7,5)).endDate(LocalDate.of(2026,7,31)).sponsorLabel("Sponsored by ABC").approvalStatus(CampaignApprovalStatus.PENDING).createdAt(Instant.now()).updatedAt(Instant.now()).build();}
}
