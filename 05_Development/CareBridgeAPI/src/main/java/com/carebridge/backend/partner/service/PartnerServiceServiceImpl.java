package com.carebridge.backend.partner.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.SubmitServiceListingRequest;
import com.carebridge.backend.partner.dto.response.SubmitServiceListingResponse;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import com.carebridge.backend.partner.repository.PartnerServiceRepository;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.partner.entity.OrganizationStatus;
import com.carebridge.backend.partner.entity.PartnerService;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.mapper.PartnerServiceMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class PartnerServiceServiceImpl implements PartnerServiceService {
    private final PartnerOrganizationRepository organizationRepository;
    private final PartnerServiceRepository serviceRepository;
    private final AuditService auditService;
    private final PartnerServiceMapper mapper;

    @Override @Transactional
    public SubmitServiceListingResponse submitService(SubmitServiceListingRequest request, UUID actorId) {
        var organization = organizationRepository.findByRepresentativeUserId(actorId)
                .orElseThrow(PartnerException::organizationNotFound);
        if (organization.getStatus() != OrganizationStatus.APPROVED) {
            throw PartnerException.organizationNotApproved();
        }
        PartnerService saved = serviceRepository.save(mapper.toEntity(request, organization.getId()));
        auditService.log(AuditAction.PARTNER_SERVICE_SUBMITTED, actorId, "PartnerService",
                saved.getId().toString(), "submitted");
        return mapper.toResponse(saved);
    }
}
