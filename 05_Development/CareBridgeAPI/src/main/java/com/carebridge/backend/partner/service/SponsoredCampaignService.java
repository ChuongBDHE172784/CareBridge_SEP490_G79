package com.carebridge.backend.partner.service;
import com.carebridge.backend.partner.dto.request.SubmitSponsoredContentRequest;
import com.carebridge.backend.partner.dto.response.SubmitSponsoredContentResponse;
import java.util.UUID;
public interface SponsoredCampaignService { SubmitSponsoredContentResponse submitCampaign(SubmitSponsoredContentRequest request,UUID actorId); }
