package com.carebridge.backend.expertverification.repository;

import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Canonical identity persistence backed exclusively by expert_credentials and attachments.
 *
 * <p>The root IDENTITY_DOCUMENT credential is the public attempt id. Every evidence image is
 * stored as another credential in the same credential-number group, with file_id referencing
 * attachments. Pipeline details live on the root credential's review_note and review decisions
 * use the canonical credential review columns.</p>
 */
@Repository
@RequiredArgsConstructor
public class ExpertIdentityVerificationRepository {

    public static final String ROOT_TYPE = "IDENTITY_VERIFICATION";
    private static final String SELFIE_TYPE = "IDENTITY_SELFIE";
    private static final String BACK_TYPE = "IDENTITY_DOCUMENT_BACK";
    private static final String SELFIE_CROP_TYPE = "IDENTITY_SELFIE_CROP";
    private static final String CARD_CROP_TYPE = "IDENTITY_DOCUMENT_CROP";

    private final ExpertCredentialRepository credentials;
    private final ObjectMapper objectMapper;

    public List<ExpertIdentityVerification> findByExpertProfileIdOrderByCreatedAtDesc(UUID profileId) {
        return credentials.findByExpertProfileIdAndCredentialTypeOrderByCreatedAtDesc(profileId, ROOT_TYPE)
                .stream().map(this::assemble).toList();
    }

    public Optional<ExpertIdentityVerification> findFirstByExpertProfileIdOrderByCreatedAtDesc(
            UUID profileId) {
        return findByExpertProfileIdOrderByCreatedAtDesc(profileId).stream().findFirst();
    }

    public List<ExpertIdentityVerification> findByReviewStatusInOrderByCreatedAtAsc(
            List<IdentityReviewStatus> statuses) {
        return credentials.findByCredentialTypeAndReviewStatusOrderByCreatedAtAsc(
                        ROOT_TYPE, ReviewStatus.PENDING)
                .stream().map(this::assemble)
                .filter(attempt -> statuses.contains(attempt.getReviewStatus()))
                .toList();
    }

    public Optional<ExpertIdentityVerification> findByIdForUpdate(UUID id) {
        return credentials.findByCredentialIdForUpdate(id)
                .filter(c -> ROOT_TYPE.equals(c.getCredentialType()))
                .map(this::assemble);
    }

    public ExpertIdentityVerification save(ExpertIdentityVerification attempt) {
        if (attempt.getId() == null) {
            ExpertCredential root = credentials.save(ExpertCredential.builder()
                    .expertProfileId(attempt.getExpertProfileId())
                    .credentialType(ROOT_TYPE)
                    .issuer(attempt.getFaceProvider())
                    .fileId(attempt.getIdentityFrontFileId())
                    .reviewStatus(toCredentialStatus(attempt.getReviewStatus()))
                    .build());
            attempt.setId(root.getCredentialId());
            root.setCredentialNumber(root.getCredentialId().toString());
            root.setReviewNote(writeMetadata(attempt));
            credentials.save(root);
            saveEvidence(attempt, SELFIE_TYPE, attempt.getSelfieFileId());
            saveEvidence(attempt, BACK_TYPE, attempt.getIdentityBackFileId());
            attempt.setCreatedAt(toInstant(root));
            return attempt;
        }

        ExpertCredential root = credentials.findByCredentialIdForUpdate(attempt.getId())
                .filter(c -> ROOT_TYPE.equals(c.getCredentialType()))
                .orElseThrow(() -> new IllegalStateException(
                        "Canonical identity credential not found: " + attempt.getId()));
        root.setReviewStatus(toCredentialStatus(attempt.getReviewStatus()));
        root.setReviewNote(writeMetadata(attempt));
        root.setReviewedBy(attempt.getReviewedBy());
        root.setReviewedAt(attempt.getReviewedAt() == null ? null
                : java.time.LocalDateTime.ofInstant(attempt.getReviewedAt(), ZoneOffset.UTC));
        credentials.save(root);
        upsertEvidence(attempt, SELFIE_CROP_TYPE, attempt.getSelfieCropFileId());
        upsertEvidence(attempt, CARD_CROP_TYPE, attempt.getIdCardCropFileId());
        return attempt;
    }

    private ExpertIdentityVerification assemble(ExpertCredential root) {
        IdentityMetadata metadata = readMetadata(root.getReviewNote());
        List<ExpertCredential> evidence = credentials.findByExpertProfileIdAndCredentialNumber(
                root.getExpertProfileId(), root.getCredentialId().toString());
        return ExpertIdentityVerification.builder()
                .id(root.getCredentialId())
                .expertProfileId(root.getExpertProfileId())
                .identityFrontFileId(root.getFileId())
                .selfieFileId(fileId(evidence, SELFIE_TYPE))
                .identityBackFileId(fileId(evidence, BACK_TYPE))
                .selfieCropFileId(fileId(evidence, SELFIE_CROP_TYPE))
                .idCardCropFileId(fileId(evidence, CARD_CROP_TYPE))
                .faceProvider(root.getIssuer())
                .faceStatus(metadata.faceStatus())
                .faceSimilarity(metadata.faceSimilarity())
                .faceThreshold(metadata.faceThreshold())
                .providerErrorCode(metadata.providerErrorCode())
                .reviewStatus(fromCredentialStatus(root.getReviewStatus()))
                .reviewReason(metadata.reviewReason())
                .reviewedBy(root.getReviewedBy())
                .reviewedAt(root.getReviewedAt() == null ? null
                        : root.getReviewedAt().toInstant(ZoneOffset.UTC))
                .createdAt(toInstant(root))
                .detectionSelfieStatus(metadata.detectionSelfieStatus())
                .detectionIdCardStatus(metadata.detectionIdCardStatus())
                .pipelineErrorCode(metadata.pipelineErrorCode())
                .pipelineStatus(metadata.pipelineStatus())
                .processedAt(metadata.processedAt())
                .build();
    }

    private void saveEvidence(ExpertIdentityVerification attempt, String type, UUID fileId) {
        if (fileId == null) return;
        credentials.save(ExpertCredential.builder()
                .expertProfileId(attempt.getExpertProfileId())
                .credentialType(type)
                .credentialNumber(attempt.getId().toString())
                .fileId(fileId)
                .reviewStatus(toCredentialStatus(attempt.getReviewStatus()))
                .build());
    }

    private void upsertEvidence(ExpertIdentityVerification attempt, String type, UUID fileId) {
        if (fileId == null) return;
        credentials.findByExpertProfileIdAndCredentialNumber(
                        attempt.getExpertProfileId(), attempt.getId().toString())
                .stream()
                .filter(credential -> type.equals(credential.getCredentialType()))
                .findFirst()
                .ifPresentOrElse(credential -> {
                    credential.setFileId(fileId);
                    credentials.save(credential);
                }, () -> saveEvidence(attempt, type, fileId));
    }

    private static UUID fileId(List<ExpertCredential> evidence, String type) {
        return evidence.stream().filter(c -> type.equals(c.getCredentialType()))
                .map(ExpertCredential::getFileId).findFirst().orElse(null);
    }

    private String writeMetadata(ExpertIdentityVerification attempt) {
        try {
            return objectMapper.writeValueAsString(new IdentityMetadata(
                    attempt.getFaceStatus(), attempt.getFaceSimilarity(), attempt.getFaceThreshold(),
                    attempt.getProviderErrorCode(), attempt.getReviewStatus(), attempt.getReviewReason(),
                    attempt.getDetectionSelfieStatus(), attempt.getDetectionIdCardStatus(),
                    attempt.getPipelineErrorCode(), attempt.getPipelineStatus(), attempt.getProcessedAt()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to persist identity review metadata", ex);
        }
    }

    private IdentityMetadata readMetadata(String value) {
        if (value == null || value.isBlank()) {
            return new IdentityMetadata(null, null, null, null, null, null,
                    null, null, null, null, null);
        }
        try {
            return objectMapper.readValue(value, IdentityMetadata.class);
        } catch (JsonProcessingException ex) {
            // IDENTITY_DOCUMENT used to allow human review notes. Treat those legacy notes as
            // non-structured metadata instead of breaking onboarding/review reads.
            return new IdentityMetadata(null, null, null, null, null, value,
                    null, null, null, null, null);
        }
    }

    private static Instant toInstant(ExpertCredential credential) {
        return credential.getCreatedAt() == null ? null
                : credential.getCreatedAt().toInstant(ZoneOffset.UTC);
    }

    private static ReviewStatus toCredentialStatus(IdentityReviewStatus status) {
        if (status == IdentityReviewStatus.APPROVED) return ReviewStatus.APPROVED;
        if (status == IdentityReviewStatus.REJECTED) return ReviewStatus.REJECTED;
        return ReviewStatus.PENDING;
    }

    private static IdentityReviewStatus fromCredentialStatus(ReviewStatus status) {
        if (status == ReviewStatus.APPROVED) return IdentityReviewStatus.APPROVED;
        if (status == ReviewStatus.REJECTED) return IdentityReviewStatus.REJECTED;
        return IdentityReviewStatus.MANUAL_REVIEW_REQUIRED;
    }

    private record IdentityMetadata(
            String faceStatus,
            java.math.BigDecimal faceSimilarity,
            java.math.BigDecimal faceThreshold,
            String providerErrorCode,
            IdentityReviewStatus reviewStatus,
            String reviewReason,
            String detectionSelfieStatus,
            String detectionIdCardStatus,
            String pipelineErrorCode,
            String pipelineStatus,
            java.time.Instant processedAt) {}
}
