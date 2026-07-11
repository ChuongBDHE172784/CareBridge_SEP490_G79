package com.carebridge.backend.partner.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.partner.dto.request.CreatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.CreatePartnerProfileResponse;
import com.carebridge.backend.partner.dto.request.UpdatePartnerProfileRequest;
import com.carebridge.backend.partner.dto.response.UpdatePartnerProfileResponse;
import com.carebridge.backend.partner.entity.PartnerOrganization;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.partner.mapper.PartnerProfileMapper;
import com.carebridge.backend.partner.repository.PartnerOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerProfileServiceImpl implements PartnerProfileService {

    private final PartnerOrganizationRepository partnerOrganizationRepository;
    private final PartnerProfileMapper partnerProfileMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public CreatePartnerProfileResponse createProfile(CreatePartnerProfileRequest request, java.util.UUID actorId) {
        // Oracle: ADR-001, BR-PTR-001 — C4: check duplicate before save
        if (partnerOrganizationRepository.findByRepresentativeUserId(actorId).isPresent()) {
            throw PartnerException.profileAlreadyExists();
        }

        // Oracle: ADR-001 — email must be unique across organizations
        if (partnerOrganizationRepository.existsByEmail(request.getEmail())) {
            throw PartnerException.emailAlreadyRegistered();
        }

        PartnerOrganization entity = partnerProfileMapper.toEntity(request, actorId);

        PartnerOrganization saved;
        try {
            saved = partnerOrganizationRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Oracle: ADR-001 — DB unique constraint is safety net for concurrent requests
            throw PartnerException.profileAlreadyExists();
        }

        // Oracle: ADR-004, BR-AUDIT-002 — C5: audit AFTER successful save only
        auditService.log(
                AuditAction.PARTNER_PROFILE_CREATED,
                actorId,
                "PartnerOrganization",
                saved.getId().toString(),
                "created");

        return partnerProfileMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UpdatePartnerProfileResponse updateProfile(UpdatePartnerProfileRequest request, java.util.UUID actorId) {
        PartnerOrganization entity = partnerOrganizationRepository.findByRepresentativeUserId(actorId)
                .orElseThrow(PartnerException::profileNotFound);

        if (entity.getStatus() == com.carebridge.backend.partner.entity.OrganizationStatus.SUSPENDED
                || entity.getStatus() == com.carebridge.backend.partner.entity.OrganizationStatus.REJECTED) {
            throw PartnerException.profileNotEditable();
        }

        partnerProfileMapper.updateEntity(request, entity);
        PartnerOrganization saved = partnerOrganizationRepository.save(entity);

        auditService.log(
                AuditAction.PARTNER_PROFILE_UPDATED,
                actorId,
                "PartnerOrganization",
                saved.getId().toString(),
                "updated");

        return partnerProfileMapper.toUpdateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UpdatePartnerProfileResponse getOwnProfile(java.util.UUID actorId) {
        return partnerOrganizationRepository.findByRepresentativeUserId(actorId)
                .map(partnerProfileMapper::toUpdateResponse)
                .orElseThrow(PartnerException::profileNotFound);
    }
}
