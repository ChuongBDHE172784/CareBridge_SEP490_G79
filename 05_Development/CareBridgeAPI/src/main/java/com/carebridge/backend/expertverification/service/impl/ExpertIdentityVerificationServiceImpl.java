package com.carebridge.backend.expertverification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertverification.adapter.FaceVerificationAdapter;
import com.carebridge.backend.expertverification.adapter.FaceVerificationResult;
import com.carebridge.backend.expertverification.dto.request.ReviewIdentityRequest;
import com.carebridge.backend.expertverification.dto.response.ExpertOnboardingResponse;
import com.carebridge.backend.expertverification.dto.response.IdentityVerificationResponse;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.expertverification.service.IExpertIdentityVerificationService;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.dto.ViewFileResponse;
import com.carebridge.backend.file.service.IFileService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class ExpertIdentityVerificationServiceImpl implements IExpertIdentityVerificationService {

    private static final long MAX_IDENTITY_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ExpertProfileRepository profileRepository;
    private final ExpertIdentityVerificationRepository identityRepository;
    private final ExpertCredentialRepository credentialRepository;
    private final FaceVerificationAdapter faceVerificationAdapter;
    private final IFileService fileService;
    private final AuditService auditService;

    @Override
    public IdentityVerificationResponse submit(
            UUID userId, MultipartFile selfie, MultipartFile identityFront, MultipartFile identityBack) {
        var profile = profileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-002", "Expert profile not found"));

        var existingAttempt = identityRepository
                .findFirstByExpertProfileIdOrderByCreatedAtDesc(profile.getExpertProfileId());
        if (existingAttempt.isPresent()
                && existingAttempt.get().getReviewStatus() != IdentityReviewStatus.REJECTED) {
            return toResponse(existingAttempt.get());
        }

        byte[] selfieBytes = validateIdentityImage(selfie, "selfie");
        byte[] frontBytes = validateIdentityImage(identityFront, "identityFront");
        validateIdentityImage(identityBack, "identityBack");

        FaceVerificationResult faceResult = faceVerificationAdapter.verify(
                selfieBytes, normalizedMime(selfie), frontBytes, normalizedMime(identityFront));
        if (faceResult.status() == FaceVerificationStatus.NO_FACE
                || faceResult.status() == FaceVerificationStatus.MULTIPLE_FACES) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "EXPIDENT-004",
                    "Exactly one clear face is required in the selfie and identity front image");
        }

        List<UUID> uploaded = new ArrayList<>();
        try {
            UploadFileResponse selfieUpload = fileService.uploadPrivateFile(selfie, userId);
            uploaded.add(selfieUpload.getFileId());
            UploadFileResponse frontUpload = fileService.uploadPrivateFile(identityFront, userId);
            uploaded.add(frontUpload.getFileId());
            UploadFileResponse backUpload = fileService.uploadPrivateFile(identityBack, userId);
            uploaded.add(backUpload.getFileId());

            IdentityReviewStatus reviewStatus = switch (faceResult.status()) {
                case MATCHED -> IdentityReviewStatus.PENDING_REVIEW;
                case NOT_MATCHED -> IdentityReviewStatus.REJECTED;
                case DISABLED, RETRYABLE_ERROR -> IdentityReviewStatus.MANUAL_REVIEW_REQUIRED;
                case NO_FACE, MULTIPLE_FACES -> throw new IllegalStateException("Validated above");
            };
            String reviewReason = faceResult.status() == FaceVerificationStatus.NOT_MATCHED
                    ? "Face similarity is below the configured threshold" : null;
            ExpertIdentityVerification saved = identityRepository.save(
                    ExpertIdentityVerification.builder()
                            .expertProfileId(profile.getExpertProfileId())
                            .selfieFileId(selfieUpload.getFileId())
                            .identityFrontFileId(frontUpload.getFileId())
                            .identityBackFileId(backUpload.getFileId())
                            .faceProvider("COMPREFACE")
                            .faceStatus(faceResult.status())
                            .faceSimilarity(faceResult.similarity())
                            .faceThreshold(faceResult.threshold())
                            .providerErrorCode(faceResult.providerErrorCode())
                            .reviewStatus(reviewStatus)
                            .reviewReason(reviewReason)
                            .build());
            auditService.log(AuditAction.EXPERT_VERIFICATION, userId,
                    "ExpertIdentityVerification", saved.getId().toString(),
                    Map.of("event", "IDENTITY_SUBMITTED", "faceStatus", faceResult.status().name()));
            return toResponse(saved);
        } catch (RuntimeException ex) {
            uploaded.forEach(fileId -> safePurge(fileId, userId));
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertOnboardingResponse getOnboarding(UUID userId) {
        var profileOptional = profileRepository.findByUserId(userId);
        if (profileOptional.isEmpty()) {
            return ExpertOnboardingResponse.builder()
                    .profileExists(false)
                    .identityStatus("MISSING")
                    .credentialStatus("MISSING")
                    .nextStep("PROFILE")
                    .build();
        }

        var profile = profileOptional.get();
        var latest = identityRepository
                .findFirstByExpertProfileIdOrderByCreatedAtDesc(profile.getExpertProfileId());
        List<ExpertCredential> credentials =
                credentialRepository.findByExpertProfileId(profile.getExpertProfileId());
        String credentialStatus = credentialStatus(credentials);
        String identityStatus = latest.map(attempt -> attempt.getReviewStatus().name()).orElse("MISSING");
        String nextStep = determineNextStep(profile.getVerificationStatus(), identityStatus, credentialStatus);

        return ExpertOnboardingResponse.builder()
                .profileExists(true)
                .identityStatus(identityStatus)
                .credentialStatus(credentialStatus)
                .verificationStatus(profile.getVerificationStatus())
                .nextStep(nextStep)
                .latestIdentityAttempt(latest.map(this::toResponse).orElse(null))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ViewFileResponse getAuthorizedFileUrl(UUID fileId, UUID callerId) {
        return fileService.viewFile(fileId, callerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityVerificationResponse> getPendingReviews() {
        return identityRepository.findByReviewStatusInOrderByCreatedAtAsc(List.of(
                        IdentityReviewStatus.PENDING_REVIEW,
                        IdentityReviewStatus.MANUAL_REVIEW_REQUIRED))
                .stream().map(this::toResponse).toList();
    }

    @Override
    public IdentityVerificationResponse review(
            UUID attemptId, ReviewIdentityRequest request, UUID reviewerId) {
        if (request.getReviewStatus() != IdentityReviewStatus.APPROVED
                && request.getReviewStatus() != IdentityReviewStatus.REJECTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPIDENT-006",
                    "Review decision must be APPROVED or REJECTED");
        }
        if (request.getReviewStatus() == IdentityReviewStatus.REJECTED
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPIDENT-007",
                    "A rejection reason is required");
        }

        ExpertIdentityVerification attempt = identityRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPIDENT-404", "Identity verification not found"));
        if (attempt.getReviewStatus() == request.getReviewStatus()) {
            return toResponse(attempt);
        }
        if (attempt.getReviewStatus() == IdentityReviewStatus.APPROVED
                || attempt.getReviewStatus() == IdentityReviewStatus.REJECTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "EXPIDENT-008",
                    "Identity verification already has a final decision");
        }

        attempt.setReviewStatus(request.getReviewStatus());
        attempt.setReviewReason(request.getReason());
        attempt.setReviewedBy(reviewerId);
        attempt.setReviewedAt(Instant.now());
        ExpertIdentityVerification saved = identityRepository.save(attempt);
        auditService.log(AuditAction.EXPERT_VERIFICATION, reviewerId,
                "ExpertIdentityVerification", saved.getId().toString(),
                Map.of("event", "IDENTITY_REVIEWED", "decision", request.getReviewStatus().name()));
        return toResponse(saved);
    }

    private byte[] validateIdentityImage(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPIDENT-001",
                    field + " is required");
        }
        if (file.getSize() > MAX_IDENTITY_IMAGE_BYTES) {
            throw new BusinessException(HttpStatus.CONTENT_TOO_LARGE, "EXPIDENT-002",
                    field + " exceeds 5MB");
        }
        try {
            byte[] bytes = file.getBytes();
            boolean jpeg = bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
            boolean png = bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4e && bytes[3] == 0x47;
            if (!jpeg && !png) {
                throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "EXPIDENT-003",
                        field + " must be a JPEG or PNG image");
            }
            return bytes;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPIDENT-003",
                    "Unable to read " + field);
        }
    }

    private static String normalizedMime(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    private void safePurge(UUID fileId, UUID userId) {
        try {
            fileService.purgeFile(fileId, userId);
        } catch (RuntimeException ignored) {
            // Preserve the original workflow failure. Orphan cleanup remains observable via upload audit.
        }
    }

    private static String credentialStatus(List<ExpertCredential> credentials) {
        List<ExpertCredential> professional = credentials.stream()
                .filter(c -> !"IDENTITY_DOCUMENT".equals(c.getCredentialType()))
                .toList();
        if (professional.stream().anyMatch(c -> c.getReviewStatus() == ReviewStatus.APPROVED
                && (c.getExpiryDate() == null || !c.getExpiryDate().isBefore(java.time.LocalDate.now())))) {
            return "APPROVED";
        }
        if (professional.stream().anyMatch(c -> c.getReviewStatus() == ReviewStatus.PENDING)) return "PENDING";
        if (!professional.isEmpty()) return "REJECTED";
        return "MISSING";
    }

    private static String determineNextStep(
            VerificationStatus verificationStatus, String identityStatus, String credentialStatus) {
        if (verificationStatus == VerificationStatus.APPROVED) return "COMPLETE";
        if ("MISSING".equals(identityStatus) || "REJECTED".equals(identityStatus)) return "IDENTITY";
        if ("MISSING".equals(credentialStatus) || "REJECTED".equals(credentialStatus)) return "CREDENTIAL";
        return "UNDER_REVIEW";
    }

    private IdentityVerificationResponse toResponse(ExpertIdentityVerification entity) {
        return IdentityVerificationResponse.builder()
                .identityVerificationId(entity.getId())
                .expertProfileId(entity.getExpertProfileId())
                .selfieFileId(entity.getSelfieFileId())
                .identityFrontFileId(entity.getIdentityFrontFileId())
                .identityBackFileId(entity.getIdentityBackFileId())
                .faceStatus(entity.getFaceStatus())
                .faceSimilarity(entity.getFaceSimilarity())
                .faceThreshold(entity.getFaceThreshold())
                .providerErrorCode(entity.getProviderErrorCode())
                .reviewStatus(entity.getReviewStatus())
                .reviewReason(entity.getReviewReason())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
