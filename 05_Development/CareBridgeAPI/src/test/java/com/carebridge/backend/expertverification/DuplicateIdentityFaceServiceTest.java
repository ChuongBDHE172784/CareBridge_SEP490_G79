package com.carebridge.backend.expertverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.carebridge.backend.expertverification.adapter.FaceVerificationAdapter;
import com.carebridge.backend.expertverification.adapter.FaceVerificationResult;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.service.impl.DuplicateIdentityFaceService;
import com.carebridge.backend.file.entity.FileStatus;
import com.carebridge.backend.file.entity.UploadedFile;
import com.carebridge.backend.file.repository.UploadedFileRepository;
import com.carebridge.backend.file.service.IStorageService;
import com.carebridge.backend.file.service.StorageServiceResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateIdentityFaceServiceTest {

    @Mock private ExpertIdentityVerificationRepository identityRepository;
    @Mock private UploadedFileRepository fileRepository;
    @Mock private StorageServiceResolver storageServiceResolver;
    @Mock private FaceVerificationAdapter faceVerificationAdapter;
    @Mock private IStorageService storageService;

    private DuplicateIdentityFaceService service;

    @BeforeEach
    void setUp() {
        service = new DuplicateIdentityFaceService(
                identityRepository, fileRepository, storageServiceResolver,
                faceVerificationAdapter);
    }

    @Test
    void matchedReferenceReturnsPossibleDuplicate() {
        UUID currentProfileId = UUID.randomUUID();
        UUID matchedProfileId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        UUID referenceFileId = UUID.randomUUID();
        byte[] selfie = {1, 2, 3};
        byte[] reference = {4, 5, 6};

        var candidate = ExpertIdentityVerification.builder()
                .id(attemptId)
                .expertProfileId(matchedProfileId)
                .idCardCropFileId(referenceFileId)
                .build();
        var file = UploadedFile.builder()
                .id(referenceFileId)
                .storageKey("candidate|image|AUTHENTICATED")
                .mimeType("image/jpeg")
                .fileSizeBytes(reference.length)
                .status(FileStatus.ACTIVE)
                .build();

        when(identityRepository.findFaceDuplicateCandidates(currentProfileId))
                .thenReturn(List.of(candidate));
        when(fileRepository.findByIdAndStatus(referenceFileId, FileStatus.ACTIVE))
                .thenReturn(Optional.of(file));
        when(storageServiceResolver.resolve("cloudinary")).thenReturn(storageService);
        when(storageService.read(file.getStorageKey(), 5L * 1024 * 1024))
                .thenReturn(reference);
        when(faceVerificationAdapter.verify(
                selfie, "image/jpeg", reference, "image/jpeg"))
                .thenReturn(new FaceVerificationResult(
                        FaceVerificationStatus.MATCHED,
                        new BigDecimal("0.93"),
                        new BigDecimal("0.75"),
                        null));

        var result = service.findPossibleDuplicate(
                currentProfileId, selfie, "image/jpeg");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().expertProfileId()).isEqualTo(matchedProfileId);
        assertThat(result.orElseThrow().similarity()).isEqualByComparingTo("0.93");
    }
}
