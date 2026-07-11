package com.carebridge.backend.partner.service;
import com.carebridge.backend.partner.dto.request.PartnerDecisionRequest;import com.carebridge.backend.partner.dto.response.PartnerDecisionResponse;import java.util.UUID;
public interface PartnerApprovalService{PartnerDecisionResponse decide(UUID partnerId,PartnerDecisionRequest request,UUID adminId);}
