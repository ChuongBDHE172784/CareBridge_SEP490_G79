package com.carebridge.backend.partner.dto.response;

import com.carebridge.backend.partner.entity.CampaignApprovalStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SponsoredCampaignListItemResponse(UUID id, String title, String description,
        LocalDate startDate, LocalDate endDate, String sponsorLabel, CampaignApprovalStatus approvalStatus,
        Instant createdAt) {}
