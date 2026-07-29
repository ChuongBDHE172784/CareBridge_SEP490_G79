package com.carebridge.backend.expertverification.service.impl;

import com.carebridge.backend.expertverification.adapter.FaceVerificationAdapter;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.StorageServiceResolver;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Screens a new identity selfie against existing verified document-face crops.
 *
 * <p>This deliberately reuses expert_credentials and attachments. It does not persist biometric
 * templates or introduce another database table. A match is only a review signal; the caller must
 * not automatically reject or merge accounts based on face similarity alone.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateIdentityFaceService {

    private static final long MAX_REFERENCE_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ExpertIdentityVerificationRepository identityRepository;
    private final UploadedFileRepository fileRepository;
    private final StorageServiceResolver storageServiceResolver;
    private final FaceVerificationAdapter faceVerificationAdapter;

    @Value("${carebridge.compreface.duplicate-check-enabled:true}")
    private boolean enabled = true;

    @Value("${carebridge.compreface.duplicate-check-max-candidates:200}")
    private int maxCandidates = 200;

    public Optional<DuplicateFaceMatch> findPossibleDuplicate(
            UUID currentProfileId, byte[] croppedSelfie, String selfieMimeType) {
        if (!enabled || croppedSelfie == null || croppedSelfie.length == 0) {
            return Optional.empty();
        }

        return identityRepository.findFaceDuplicateCandidates(currentProfileId).stream()
                .limit(Math.max(0, maxCandidates))
                .map(candidate -> compare(candidate, croppedSelfie, selfieMimeType))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<DuplicateFaceMatch> compare(
            ExpertIdentityVerification candidate, byte[] croppedSelfie, String selfieMimeType) {
        UUID referenceFileId = candidate.getIdCardCropFileId();
        try {
            UploadedFile reference = fileRepository
                    .findByIdAndStatus(referenceFileId, FileStatus.ACTIVE)
                    .filter(file -> file.getMimeType() != null
                            && file.getMimeType().startsWith("image/"))
                    .filter(file -> file.getFileSizeBytes() <= MAX_REFERENCE_IMAGE_BYTES)
                    .orElse(null);
            if (reference == null) {
                return Optional.empty();
            }

            byte[] referenceBytes = storageServiceResolver.resolve("cloudinary")
                    .read(reference.getStorageKey(), MAX_REFERENCE_IMAGE_BYTES);
            var result = faceVerificationAdapter.verify(
                    croppedSelfie,
                    selfieMimeType == null ? "image/jpeg" : selfieMimeType,
                    referenceBytes,
                    reference.getMimeType());

            if (result.status() != FaceVerificationStatus.MATCHED) {
                return Optional.empty();
            }
            return Optional.of(new DuplicateFaceMatch(
                    candidate.getExpertProfileId(),
                    candidate.getId(),
                    result.similarity(),
                    result.threshold()));
        } catch (RuntimeException ex) {
            // Duplicate screening must not turn a temporary storage/provider issue into a failed
            // identity submission. The normal human review flow remains available.
            log.warn("Unable to screen identity attempt {} for duplicate face: {}",
                    candidate.getId(), ex.getMessage());
            return Optional.empty();
        }
    }

    public record DuplicateFaceMatch(
            UUID expertProfileId,
            UUID identityVerificationId,
            BigDecimal similarity,
            BigDecimal threshold) {
    }
}
