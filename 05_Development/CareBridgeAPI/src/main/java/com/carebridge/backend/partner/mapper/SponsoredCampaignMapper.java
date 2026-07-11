package com.carebridge.backend.partner.mapper;
import com.carebridge.backend.partner.dto.request.SubmitSponsoredContentRequest;
import com.carebridge.backend.partner.dto.response.SubmitSponsoredContentResponse;
import com.carebridge.backend.partner.entity.*;
import java.util.UUID;
import org.springframework.stereotype.Component;
@Component public class SponsoredCampaignMapper{
 public SponsoredCampaign toEntity(SubmitSponsoredContentRequest r,UUID partnerId){return SponsoredCampaign.builder().partnerId(partnerId).title(r.title()).description(r.description()).startDate(r.startDate()).endDate(r.endDate()).sponsorLabel(r.sponsorLabel()).approvalStatus(CampaignApprovalStatus.PENDING).reviewedBy(null).build();}
 public SubmitSponsoredContentResponse toResponse(SponsoredCampaign e){return new SubmitSponsoredContentResponse(e.getId(),e.getPartnerId(),e.getTitle(),e.getDescription(),e.getStartDate(),e.getEndDate(),e.getSponsorLabel(),e.getApprovalStatus(),e.getCreatedAt());}
}
