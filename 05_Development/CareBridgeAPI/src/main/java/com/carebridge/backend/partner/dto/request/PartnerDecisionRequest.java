package com.carebridge.backend.partner.dto.request;
import com.carebridge.backend.partner.entity.PartnerDecision;import jakarta.validation.constraints.NotNull;
public record PartnerDecisionRequest(@NotNull PartnerDecision decision,String reason){}
