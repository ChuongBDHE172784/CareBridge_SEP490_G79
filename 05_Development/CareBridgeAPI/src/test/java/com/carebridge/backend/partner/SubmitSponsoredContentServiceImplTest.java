package com.carebridge.backend.partner;
import static com.carebridge.backend.partner.SubmitSponsoredContentTestFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.entity.*;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.repository.*;
import com.carebridge.backend.partner.service.SponsoredCampaignServiceImpl;
import com.carebridge.backend.partner.mapper.SponsoredCampaignMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class) class SubmitSponsoredContentServiceImplTest {
 @Mock PartnerOrganizationRepository orgRepo; @Mock SponsoredCampaignRepository campaignRepo; @Mock AuditService audit; @Spy SponsoredCampaignMapper mapper=new SponsoredCampaignMapper(); @InjectMocks SponsoredCampaignServiceImpl service;
 void approved(){when(orgRepo.findByRepresentativeUserId(OWNER)).thenReturn(Optional.of(org(OrganizationStatus.APPROVED)));when(campaignRepo.save(any())).thenReturn(saved());}
 @Test void sscTc401_createsPendingUnreviewedCampaign(){approved();var r=service.submitCampaign(request(),OWNER);ArgumentCaptor<SponsoredCampaign> c=ArgumentCaptor.forClass(SponsoredCampaign.class);verify(campaignRepo).save(c.capture());assertAll(()->assertEquals(ORG,c.getValue().getPartnerId()),()->assertEquals(CampaignApprovalStatus.PENDING,c.getValue().getApprovalStatus()),()->assertNull(c.getValue().getReviewedBy()),()->assertEquals(CampaignApprovalStatus.PENDING,r.approvalStatus()));}
 @Test void sscTc402_missingOrgThrowsPtr013(){when(orgRepo.findByRepresentativeUserId(OWNER)).thenReturn(Optional.empty());assertEquals("PTR-013",assertThrows(PartnerException.class,()->service.submitCampaign(request(),OWNER)).getCode());}
 @Test void sscTc403_nonApprovedThrowsPtr014(){for(var s:new OrganizationStatus[]{OrganizationStatus.PENDING_APPROVAL,OrganizationStatus.SUSPENDED,OrganizationStatus.REJECTED}){reset(orgRepo,campaignRepo);when(orgRepo.findByRepresentativeUserId(OWNER)).thenReturn(Optional.of(org(s)));assertEquals("PTR-014",assertThrows(PartnerException.class,()->service.submitCampaign(request(),OWNER)).getCode());}}
 @Test void sscTc405_serverSetsStatusAndReviewer(){approved();service.submitCampaign(request(),OWNER);ArgumentCaptor<SponsoredCampaign> c=ArgumentCaptor.forClass(SponsoredCampaign.class);verify(campaignRepo).save(c.capture());assertEquals(CampaignApprovalStatus.PENDING,c.getValue().getApprovalStatus());assertNull(c.getValue().getReviewedBy());}
 @Test void sscTc406_partnerComesFromResolvedOrg(){approved();service.submitCampaign(request(),OWNER);ArgumentCaptor<SponsoredCampaign> c=ArgumentCaptor.forClass(SponsoredCampaign.class);verify(campaignRepo).save(c.capture());assertEquals(ORG,c.getValue().getPartnerId());}
 @Test void sscTc407_auditedOnce(){approved();service.submitCampaign(request(),OWNER);verify(audit).log(eq(AuditAction.PARTNER_CAMPAIGN_SUBMITTED),eq(OWNER),eq("SponsoredCampaign"),eq(CAMPAIGN.toString()),any());}
}
