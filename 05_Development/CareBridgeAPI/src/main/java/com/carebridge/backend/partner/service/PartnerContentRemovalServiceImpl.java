package com.carebridge.backend.partner.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.RemovalRequest;
import com.carebridge.backend.partner.dto.response.RemovalResponse;
import com.carebridge.backend.partner.entity.*;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.repository.*;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerContentRemovalServiceImpl implements PartnerContentRemovalService {
    private final PartnerServiceRepository serviceRepository;
    private final SponsoredCampaignRepository campaignRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public RemovalResponse remove(PartnerContentTargetType type, UUID id, RemovalRequest request, UUID adminId) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw PartnerException.removalReasonRequired();
        }
        Instant removedAt = Instant.now();
        if (type == PartnerContentTargetType.SERVICE) {
            PartnerService target = serviceRepository.findById(id).orElseThrow(PartnerException::removalTargetNotFound);
            if (target.isRemoved()) throw PartnerException.contentAlreadyRemoved();
            target.setRemoved(true); target.setRemovedAt(removedAt); target.setRemovedBy(adminId); target.setRemovalReason(request.reason());
            serviceRepository.save(target);
        } else if (type == PartnerContentTargetType.CAMPAIGN) {
            SponsoredCampaign target = campaignRepository.findById(id).orElseThrow(PartnerException::removalTargetNotFound);
            if (target.isRemoved()) throw PartnerException.contentAlreadyRemoved();
            target.setRemoved(true); target.setRemovedAt(removedAt); target.setRemovedBy(adminId); target.setRemovalReason(request.reason());
            campaignRepository.save(target);
        } else {
            throw PartnerException.removalUnsupportedTargetType();
        }
        auditService.log(AuditAction.PARTNER_CONTENT_REMOVED, adminId, type.name(), id.toString(), "reason=" + request.reason());
        return new RemovalResponse(type, id, true, adminId, request.reason(), removedAt);
    }
}
