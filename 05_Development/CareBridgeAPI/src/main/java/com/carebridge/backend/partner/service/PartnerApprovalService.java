package com.carebridge.backend.partner.service;

import com.carebridge.backend.partner.dto.request.PartnerDecisionRequest;
import com.carebridge.backend.partner.dto.response.PartnerDecisionResponse;
import com.carebridge.backend.partner.dto.response.PartnerVerificationQueueItemResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PartnerApprovalService {
    PartnerDecisionResponse decide(UUID partnerId, PartnerDecisionRequest request, UUID adminId);
    Page<PartnerVerificationQueueItemResponse> getVerificationQueue(OrganizationStatus status, String search, Pageable pageable);
}
