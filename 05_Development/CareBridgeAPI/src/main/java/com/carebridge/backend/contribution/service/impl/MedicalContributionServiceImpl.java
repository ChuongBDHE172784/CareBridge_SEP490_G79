package com.carebridge.backend.contribution.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.contribution.dto.request.BaseAttachmentRequest;
import com.carebridge.backend.contribution.dto.request.CreateContributionRequest;
import com.carebridge.backend.contribution.dto.request.UpdateContributionRequest;
import com.carebridge.backend.contribution.dto.response.ContributionResponse;

import java.util.function.Function;
import com.carebridge.backend.contribution.entity.ContributionAttachment;
import com.carebridge.backend.contribution.entity.MedicalContribution;
import com.carebridge.backend.contribution.entity.ContributionStatus;
import com.carebridge.backend.contribution.mapper.ContributionMapper;
import com.carebridge.backend.contribution.repository.ContributionAttachmentRepository;
import com.carebridge.backend.contribution.repository.MedicalContributionRepository;
import com.carebridge.backend.contribution.service.IMedicalContributionService;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.masterdata.entity.Specialty;
import com.carebridge.backend.masterdata.entity.Hospital;
import com.carebridge.backend.masterdata.repository.SpecialtyRepository;
import com.carebridge.backend.masterdata.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalContributionServiceImpl implements IMedicalContributionService {

    private final MedicalContributionRepository contributionRepository;
    private final ContributionAttachmentRepository attachmentRepository;
    private final UploadedFileRepository fileRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final SpecialtyRepository specialtyRepository;
    private final HospitalRepository hospitalRepository;
    private final ContributionMapper contributionMapper;
    private final IFileService fileService;
    private final AuditService auditService;

    @Override
    @Transactional
    public ContributionResponse createDraft(CreateContributionRequest request, UUID expertUserId) {
        validateExpertEligibility(expertUserId);
        validateMasterData(request.getSpecialtyId(), request.getHospitalId());

        MedicalContribution contribution = MedicalContribution.builder()
                .expertUserId(expertUserId)
                .title(request.getTitle())
                .content(request.getContent())
                .specialtyId(request.getSpecialtyId())
                .hospitalId(request.getHospitalId())
                .status(ContributionStatus.DRAFT)
                .build();

        MedicalContribution saved = contributionRepository.save(contribution);

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            processAttachments(saved.getId(), request.getAttachments(), expertUserId);
        }

        auditService.log(AuditAction.CONTENT_CREATED, expertUserId,
                "MedicalContribution", saved.getId().toString(), "created");

        return contributionMapper.toResponse(saved, getAttachmentsForResponse(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionResponse getById(UUID contributionId) {
        MedicalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        return contributionMapper.toResponse(contribution, getAttachmentsForResponse(contributionId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContributionResponse> listMyContributions(UUID expertUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MedicalContribution> pageResult = contributionRepository.findByExpertUserId(expertUserId, pageable);

        return PaginatedResponse.of(pageResult.map(c -> contributionMapper.toResponse(c, getAttachmentsForResponse(c.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContributionResponse> listByStatus(ContributionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MedicalContribution> pageResult = contributionRepository.findByStatus(status, pageable);

        return PaginatedResponse.of(pageResult.map(c -> contributionMapper.toResponse(c, getAttachmentsForResponse(c.getId()))));
    }

    @Override
    @Transactional
    public ContributionResponse updateDraft(UUID contributionId, UpdateContributionRequest request, UUID expertUserId) {
        MedicalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (contribution.getStatus() != ContributionStatus.DRAFT) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRIB-001", "Only DRAFT contributions can be updated");
        }

        if (!contribution.getExpertUserId().equals(expertUserId)) {
            throw new AccessDeniedBusinessException("Not the owner of this contribution");
        }

        validateMasterData(request.getSpecialtyId(), request.getHospitalId());

        contribution.setTitle(request.getTitle());
        contribution.setContent(request.getContent());
        contribution.setSpecialtyId(request.getSpecialtyId());
        contribution.setHospitalId(request.getHospitalId());

        MedicalContribution saved = contributionRepository.save(contribution);

        // Update attachments
        if (request.getAttachments() != null) {
            attachmentRepository.deleteByContributionId(contributionId);
            processAttachments(saved.getId(), request.getAttachments(), expertUserId);
        }

        auditService.log(AuditAction.CONTENT_UPDATED, expertUserId,
                "MedicalContribution", saved.getId().toString(), "updated");

        return contributionMapper.toResponse(saved, getAttachmentsForResponse(saved.getId()));
    }

    @Override
    @Transactional
    public ContributionResponse submitForReview(UUID contributionId, UUID expertUserId) {
        MedicalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (!contribution.getExpertUserId().equals(expertUserId)) {
            throw new AccessDeniedBusinessException("Not the owner of this contribution");
        }

        if (contribution.getStatus() != ContributionStatus.DRAFT) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRIB-002", "Only DRAFT contributions can be submitted");
        }

        contribution.setStatus(ContributionStatus.SUBMITTED);
        MedicalContribution saved = contributionRepository.save(contribution);

        auditService.log(AuditAction.CONTENT_UPDATED, expertUserId,
                "MedicalContribution", saved.getId().toString(), "submitted");

        return contributionMapper.toResponse(saved, getAttachmentsForResponse(saved.getId()));
    }

    @Override
    @Transactional
    public ContributionResponse approve(UUID contributionId, UUID adminUserId) {
        MedicalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (contribution.getStatus() != ContributionStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRIB-003", "Only SUBMITTED contributions can be approved");
        }

        contribution.setStatus(ContributionStatus.APPROVED);
        MedicalContribution saved = contributionRepository.save(contribution);

        auditService.log(AuditAction.CONTENT_UPDATED, adminUserId,
                "MedicalContribution", saved.getId().toString(), "approved");

        return contributionMapper.toResponse(saved, getAttachmentsForResponse(saved.getId()));
    }

    @Override
    @Transactional
    public ContributionResponse reject(UUID contributionId, UUID adminUserId, String reason) {
        MedicalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (contribution.getStatus() != ContributionStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRIB-004", "Only SUBMITTED contributions can be rejected");
        }

        contribution.setStatus(ContributionStatus.REJECTED);
        contribution.setRejectionReason(reason);
        MedicalContribution saved = contributionRepository.save(contribution);

        auditService.log(AuditAction.CONTENT_UPDATED, adminUserId,
                "MedicalContribution", saved.getId().toString(), "rejected: " + reason);

        return contributionMapper.toResponse(saved, getAttachmentsForResponse(saved.getId()));
    }

    @Override
    @Transactional
    public void deleteDraft(UUID contributionId, UUID expertUserId) {
        MedicalContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (!contribution.getExpertUserId().equals(expertUserId)) {
            throw new AccessDeniedBusinessException("Not the owner of this contribution");
        }

        if (contribution.getStatus() != ContributionStatus.DRAFT) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRIB-005", "Only DRAFT contributions can be deleted");
        }

        attachmentRepository.deleteByContributionId(contributionId);
        contributionRepository.delete(contribution);

        auditService.log(AuditAction.CONTENT_DELETED, expertUserId,
                "MedicalContribution", contributionId.toString(), "deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligible(UUID expertUserId) {
        try {
            return expertProfileRepository.findByUserId(expertUserId)
                    .map(p -> p.getVerificationStatus() == VerificationStatus.APPROVED
                            && p.getTrustStatus() == TrustStatus.ACTIVE)
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    private void validateExpertEligibility(UUID expertUserId) {
        if (!isEligible(expertUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CONTRIB-000", "Expert not eligible: must be APPROVED and trust ACTIVE");
        }
    }

    private void validateMasterData(String specialtyId, String hospitalId) {
        if (specialtyId != null) {
            specialtyRepository.findById(specialtyId)
                    .filter(Specialty::getIsActive)
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "CONTRIB-006", "Invalid or inactive specialty"));
        }
        if (hospitalId != null) {
            hospitalRepository.findById(hospitalId)
                    .filter(Hospital::getIsActive)
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "CONTRIB-007", "Invalid or inactive hospital"));
        }
    }

    private void processAttachments(UUID contributionId,
                                    List<? extends BaseAttachmentRequest> attachments,
                                    UUID expertUserId) {
        for (BaseAttachmentRequest att : attachments) {
            UploadedFile file = fileRepository.findById(att.getFileId())
                    .orElseThrow(() -> new ResourceNotFoundException("File not found: " + att.getFileId()));

            // Validate file ownership
            if (!file.getOwnerUserId().equals(expertUserId)) {
                throw new AccessDeniedBusinessException("File does not belong to this expert");
            }

            // Validate kind matches detected MIME
            FileKind detectedKind = file.getMimeType().startsWith("image/") ? FileKind.IMAGE : FileKind.DOCUMENT;
            if (detectedKind != att.getKind()) {
                throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FILE-001", "File kind mismatch: declared " + att.getKind() +
                        " but detected " + detectedKind);
            }

            // Validate purpose is appropriate for kind
            if (att.getKind() == FileKind.IMAGE && att.getPurpose() != FilePurpose.MEDICAL_CONTRIBUTION_IMAGE) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "FILE-008", "Invalid purpose for image");
            }
            if (att.getKind() == FileKind.DOCUMENT && att.getPurpose() != FilePurpose.MEDICAL_CONTRIBUTION_DOCUMENT) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "FILE-009", "Invalid purpose for document");
            }

            ContributionAttachment attachment = ContributionAttachment.builder()
                    .contributionId(contributionId)
                    .fileId(att.getFileId())
                    .kind(att.getKind())
                    .purpose(att.getPurpose())
                    .accessMode(att.getAccessMode())
                    .displayOrder(att.getDisplayOrder())
                    .ownerUserId(expertUserId)
                    .build();
            attachmentRepository.save(attachment);
        }
    }

    private List<ContributionResponse.AttachmentResponse> getAttachmentsForResponse(UUID contributionId) {
        return attachmentRepository.findByContributionIdOrderByDisplayOrderAsc(contributionId)
                .stream()
                .map(this::mapAttachmentToResponse)
                .collect(Collectors.toList());
    }

    private ContributionResponse.AttachmentResponse mapAttachmentToResponse(ContributionAttachment attachment) {
        UploadedFile file = fileRepository.findById(attachment.getFileId())
                .orElse(null);

        String presignedUrl = null;
        if (file != null) {
            try {
                presignedUrl = fileService.viewFile(file.getId(), attachment.getOwnerUserId()).getPresignedUrl();
            } catch (Exception ignored) {
                // URL generation failed
            }
        }

        return ContributionResponse.AttachmentResponse.builder()
                .id(attachment.getId())
                .fileId(attachment.getFileId())
                .contributionId(attachment.getContributionId())
                .kind(attachment.getKind())
                .purpose(attachment.getPurpose())
                .accessMode(attachment.getAccessMode())
                .displayOrder(attachment.getDisplayOrder())
                .originalName(file != null ? file.getOriginalName() : null)
                .mimeType(file != null ? file.getMimeType() : null)
                .fileSizeBytes(file != null ? file.getFileSizeBytes() : 0)
                .presignedUrl(presignedUrl)
                .build();
    }
}