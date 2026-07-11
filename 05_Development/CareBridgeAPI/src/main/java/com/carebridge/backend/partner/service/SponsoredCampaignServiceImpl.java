package com.carebridge.backend.partner.service;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.SubmitSponsoredContentRequest;
import com.carebridge.backend.partner.dto.response.SubmitSponsoredContentResponse;
import com.carebridge.backend.partner.repository.*;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.partner.entity.*;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.mapper.SponsoredCampaignMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor public class SponsoredCampaignServiceImpl implements SponsoredCampaignService {
 private final PartnerOrganizationRepository organizationRepository; private final SponsoredCampaignRepository campaignRepository; private final AuditService auditService; private final SponsoredCampaignMapper mapper;
 @Override @Transactional public SubmitSponsoredContentResponse submitCampaign(SubmitSponsoredContentRequest request,UUID actorId){
  var org=organizationRepository.findByRepresentativeUserId(actorId).orElseThrow(PartnerException::campaignOrganizationNotFound);
  if(org.getStatus()!=OrganizationStatus.APPROVED)throw PartnerException.campaignOrganizationNotApproved();
  if(!request.isDateRangeValid())throw PartnerException.invalidCampaignDates();
  SponsoredCampaign saved=campaignRepository.save(mapper.toEntity(request,org.getId()));
  auditService.log(AuditAction.PARTNER_CAMPAIGN_SUBMITTED,actorId,"SponsoredCampaign",saved.getId().toString(),"submitted");
  return mapper.toResponse(saved);
 }
}
