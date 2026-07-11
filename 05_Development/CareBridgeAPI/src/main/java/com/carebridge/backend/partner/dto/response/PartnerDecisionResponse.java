package com.carebridge.backend.partner.dto.response;
import com.carebridge.backend.partner.entity.OrganizationStatus;import java.time.Instant;import java.util.UUID;
public record PartnerDecisionResponse(UUID partnerId,OrganizationStatus previousStatus,OrganizationStatus newStatus,UUID decidedByAdminId,String reason,Instant decidedAt){}
