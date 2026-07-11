package com.carebridge.backend.partner.dto.response;
import com.carebridge.backend.partner.entity.CampaignApprovalStatus;
import java.time.*;
import java.util.UUID;
public record SubmitSponsoredContentResponse(UUID campaignId,UUID partnerId,String title,String description,
 LocalDate startDate,LocalDate endDate,String sponsorLabel,CampaignApprovalStatus approvalStatus,Instant createdAt) {}
