package com.carebridge.backend.partner.service;
import com.carebridge.backend.partner.dto.request.SubmitSponsoredContentRequest;
import com.carebridge.backend.partner.dto.response.SubmitSponsoredContentResponse;
import java.util.UUID;
import com.carebridge.backend.partner.dto.response.SponsoredCampaignListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface SponsoredCampaignService { SubmitSponsoredContentResponse submitCampaign(SubmitSponsoredContentRequest request,UUID actorId); Page<SponsoredCampaignListItemResponse> getOwnCampaigns(UUID actorId, Pageable pageable); }
