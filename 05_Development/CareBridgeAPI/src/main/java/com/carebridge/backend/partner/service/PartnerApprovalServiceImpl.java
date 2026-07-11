package com.carebridge.backend.partner.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.PartnerDecisionRequest;
import com.carebridge.backend.partner.dto.response.PartnerDecisionResponse;
import com.carebridge.backend.partner.dto.response.PartnerVerificationQueueItemResponse;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.PartnerDecision;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerApprovalServiceImpl implements PartnerApprovalService {
    private final PartnerOrganizationRepository repository;
    private final AuditService auditService;

    @Override
    @Transactional
    public PartnerDecisionResponse decide(UUID partnerId, PartnerDecisionRequest request, UUID adminId) {
        var org = repository.findById(partnerId).orElseThrow(PartnerException::approvalPartnerNotFound);
        if ((request.decision() == PartnerDecision.REJECT || request.decision() == PartnerDecision.SUSPEND)
                && (request.reason() == null || request.reason().isBlank())) throw PartnerException.decisionReasonRequired();
        OrganizationStatus previous = org.getStatus();
        OrganizationStatus next = nextStatus(previous, request.decision());
        org.setStatus(next);
        repository.save(org);
        auditService.log(AuditAction.PARTNER_PROFILE_DECISION, adminId, "PartnerOrganization", partnerId.toString(),
                "decision=" + request.decision() + ",reason=" + request.reason());
        return new PartnerDecisionResponse(partnerId, previous, next, adminId, request.reason(), Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerVerificationQueueItemResponse> getVerificationQueue(
            OrganizationStatus status, String search, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return repository.searchVerificationQueue(status, normalizedSearch, pageable)
                .map(org -> new PartnerVerificationQueueItemResponse(org.getId(), org.getName(), org.getType(),
                        org.getStatus(), org.getCity(), org.getCreatedAt()));
    }

    private OrganizationStatus nextStatus(OrganizationStatus current, PartnerDecision decision) {
        if (current == OrganizationStatus.PENDING_APPROVAL && decision == PartnerDecision.APPROVE) return OrganizationStatus.APPROVED;
        if (current == OrganizationStatus.PENDING_APPROVAL && decision == PartnerDecision.REJECT) return OrganizationStatus.REJECTED;
        if (current == OrganizationStatus.APPROVED && decision == PartnerDecision.SUSPEND) return OrganizationStatus.SUSPENDED;
        if (current == OrganizationStatus.SUSPENDED && decision == PartnerDecision.REINSTATE) return OrganizationStatus.APPROVED;
        throw PartnerException.invalidStatusTransition();
    }
}
