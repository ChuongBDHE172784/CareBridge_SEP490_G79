package com.carebridge.backend.expertverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertverification.adapter.CompreFacePipelineAdapter;
import com.carebridge.backend.expertverification.adapter.FaceVerificationResult;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.service.impl.ExpertIdentityVerificationServiceImpl;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.service.IFileService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class ExpertIdentityVerificationServiceTest {

    @Mock private ExpertProfileRepository profileRepository;
    @Mock private ExpertIdentityVerificationRepository identityRepository;
    @Mock private ExpertCredentialRepository credentialRepository;
    @Mock private UserRepository userRepository;
    @Mock private CompreFacePipelineAdapter pipelineAdapter;
    @Mock private IFileService fileService;
    @Mock private AuditService auditService;

    private ExpertIdentityVerificationServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final UUID profileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExpertIdentityVerificationServiceImpl(
                profileRepository, identityRepository, credentialRepository, userRepository,
                pipelineAdapter, fileService, auditService,
                TransactionOperations.withoutTransaction());
    }

    @Test
    void missingImageRejectsBeforeFaceOrStorageCalls() {
        when(profileRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(profile()));
        when(identityRepository.findFirstByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(userId, null, image("front"), image("back")))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("EXPIDENT-001"));

        verifyNoInteractions(pipelineAdapter, fileService);
        verify(identityRepository).findFirstByExpertProfileIdOrderByCreatedAtDesc(profileId);
    }

    @Test
    void disabledCompreFaceStoresAllEvidenceForManualReview() {
        when(profileRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(profile()));

        // Mock pipeline to return DISABLED immediately (doesn't call CompreFace)
        when(pipelineAdapter.verifyWithPipeline(any(), any(), any(), any()))
                .thenReturn(new CompreFacePipelineAdapter.PipelineResult(
                        new FaceVerificationResult(
                                FaceVerificationStatus.DISABLED, null, BigDecimal.valueOf(.75),
                                "PENDING_LINUX_VERIFICATION"),
                        null, null,
                        com.carebridge.backend.expertverification.enums.FaceDetectionStatus.DETECTED,
                        com.carebridge.backend.expertverification.enums.FaceDetectionStatus.DETECTED,
                        "DISABLED"));

        UploadFileResponse uploadResponse = upload();
        when(fileService.uploadWithPurpose(any(), eq(userId), eq(FileKind.IMAGE), eq(FilePurpose.EXPERT_IDENTITY_SELFIE), eq(FileAccessMode.PRIVATE)))
                .thenReturn(uploadResponse);
        when(fileService.uploadWithPurpose(any(), eq(userId), eq(FileKind.IMAGE), eq(FilePurpose.EXPERT_IDENTITY_CCCD_FRONT), eq(FileAccessMode.PRIVATE)))
                .thenReturn(uploadResponse);
        when(fileService.uploadWithPurpose(any(), eq(userId), eq(FileKind.IMAGE), eq(FilePurpose.EXPERT_IDENTITY_CCCD_BACK), eq(FileAccessMode.PRIVATE)))
                .thenReturn(uploadResponse);
        when(identityRepository.save(any())).thenAnswer(invocation -> {
            ExpertIdentityVerification value = invocation.getArgument(0);
            if (value.getId() == null) {
                value.setId(UUID.randomUUID());
            }
            return value;
        });

        // Mock findByIdForUpdate to return the saved attempt
        var mockAttempt = ExpertIdentityVerification.builder()
                .id(UUID.randomUUID())
                .expertProfileId(profileId)
                .build();
        when(identityRepository.findByIdForUpdate(any())).thenReturn(Optional.of(mockAttempt));

        var response = service.submit(userId, image("selfie"), image("front"), image("back"));

        assertThat(response.getReviewStatus()).isEqualTo(IdentityReviewStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(response.getFaceStatus()).isEqualTo(FaceVerificationStatus.DISABLED);
        verify(fileService).uploadWithPurpose(any(), eq(userId), eq(FileKind.IMAGE), eq(FilePurpose.EXPERT_IDENTITY_SELFIE), eq(FileAccessMode.PRIVATE));
        verify(fileService).uploadWithPurpose(any(), eq(userId), eq(FileKind.IMAGE), eq(FilePurpose.EXPERT_IDENTITY_CCCD_FRONT), eq(FileAccessMode.PRIVATE));
        verify(fileService).uploadWithPurpose(any(), eq(userId), eq(FileKind.IMAGE), eq(FilePurpose.EXPERT_IDENTITY_CCCD_BACK), eq(FileAccessMode.PRIVATE));
        verify(identityRepository, times(2)).save(any());
    }

    @Test
    void noProfileRoutesOnboardingToProfileStep() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var onboarding = service.getOnboarding(userId);

        assertThat(onboarding.isProfileExists()).isFalse();
        assertThat(onboarding.getNextStep()).isEqualTo("PROFILE");
        assertThat(onboarding.getIdentityStatus()).isEqualTo("MISSING");
    }

    private ExpertProfile profile() {
        return ExpertProfile.builder()
                .expertProfileId(profileId)
                .userId(userId)
                .verificationStatus(VerificationStatus.PENDING)
                .build();
    }

    private static UploadFileResponse upload() {
        return UploadFileResponse.builder().fileId(UUID.randomUUID()).build();
    }

    private static MockMultipartFile image(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, 1, 2, 3});
    }
}
