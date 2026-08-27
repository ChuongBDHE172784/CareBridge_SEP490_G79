package com.carebridge.backend.expertverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertverification.adapter.CompreFacePipelineAdapter;
import com.carebridge.backend.expertverification.adapter.FaceVerificationResult;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.dto.response.DocumentReviewResponse;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.service.IExpertCredentialService;
import com.carebridge.backend.expertverification.service.impl.DuplicateIdentityFaceService;
import com.carebridge.backend.expertverification.service.impl.ExpertIdentityVerificationServiceImpl;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.enums.FileKind;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
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
    @Mock private IExpertCredentialService credentialService;
    @Mock private ExpertProfileMapper profileMapper;
    @Mock private UserRepository userRepository;
    @Mock private CareFacilityRepository careFacilityRepository;
    @Mock private CompreFacePipelineAdapter pipelineAdapter;
    @Mock private DuplicateIdentityFaceService duplicateIdentityFaceService;
    @Mock private IFileService fileService;
    @Mock private AuditService auditService;
    @Mock private com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository availabilityRepository;

    private ExpertIdentityVerificationServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final UUID profileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExpertIdentityVerificationServiceImpl(
                profileRepository, identityRepository, credentialRepository,
                credentialService, profileMapper, userRepository,
                careFacilityRepository,
                pipelineAdapter, duplicateIdentityFaceService, fileService, auditService,
                TransactionOperations.withoutTransaction(), availabilityRepository);
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
    void matchedFaceAgainstExistingIdentityRequiresManualDuplicateReview() {
        when(profileRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(profile()));
        byte[] selfieCrop = {9, 8, 7};
        byte[] cardCrop = {6, 5, 4};
        when(pipelineAdapter.verifyWithPipeline(any(), any(), any(), any()))
                .thenReturn(new CompreFacePipelineAdapter.PipelineResult(
                        new FaceVerificationResult(
                                FaceVerificationStatus.MATCHED,
                                new BigDecimal("0.91"),
                                new BigDecimal("0.75"),
                                null),
                        selfieCrop, cardCrop,
                        com.carebridge.backend.expertverification.enums.FaceDetectionStatus.DETECTED,
                        com.carebridge.backend.expertverification.enums.FaceDetectionStatus.DETECTED,
                        "MATCHED"));

        when(fileService.uploadWithPurpose(
                any(), eq(userId), eq(FileKind.IMAGE), any(), eq(FileAccessMode.PRIVATE)))
                .thenAnswer(invocation -> upload());
        when(fileService.uploadPrivateBytes(any(), eq(userId), any(), any(), any()))
                .thenAnswer(invocation -> upload());
        when(identityRepository.save(any())).thenAnswer(invocation -> {
            ExpertIdentityVerification value = invocation.getArgument(0);
            if (value.getId() == null) {
                value.setId(UUID.randomUUID());
            }
            return value;
        });

        var persistedAttempt = ExpertIdentityVerification.builder()
                .id(UUID.randomUUID())
                .expertProfileId(profileId)
                .build();
        when(identityRepository.findByIdForUpdate(any()))
                .thenReturn(Optional.of(persistedAttempt));
        when(duplicateIdentityFaceService.findPossibleDuplicate(
                eq(profileId), eq(selfieCrop), eq("image/jpeg")))
                .thenReturn(Optional.of(new DuplicateIdentityFaceService.DuplicateFaceMatch(
                        UUID.randomUUID(), UUID.randomUUID(),
                        new BigDecimal("0.93"), new BigDecimal("0.75"))));

        service.submit(userId, image("selfie"), image("front"), image("back"));

        assertThat(persistedAttempt.getReviewStatus())
                .isEqualTo(IdentityReviewStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(persistedAttempt.getReviewReason())
                .isEqualTo("Possible duplicate identity detected; admin review is required");
        verify(duplicateIdentityFaceService).findPossibleDuplicate(
                profileId, selfieCrop, "image/jpeg");
    }

    @Test
    void noProfileRoutesOnboardingToProfileStep() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var onboarding = service.getOnboarding(userId);

        assertThat(onboarding.isProfileExists()).isFalse();
        assertThat(onboarding.getNextStep()).isEqualTo("PROFILE");
        assertThat(onboarding.getIdentityStatus()).isEqualTo("MISSING");
    }

    @Test
    void adminReviewCasesGroupsCredentialEvidenceByProfile() {
        ExpertProfile profile = profile();
        DocumentReviewResponse credential = DocumentReviewResponse.builder()
                .credentialId(UUID.randomUUID())
                .expertProfileId(profileId)
                .credentialType("MEDICAL_LICENSE")
                .reviewStatus(ReviewStatus.APPROVED)
                .build();
        ExpertProfileResponse profileResponse = ExpertProfileResponse.builder()
                .expertProfileId(profileId)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(identityRepository.findFirstByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.empty());
        when(credentialService.getAdminCredentialsForProfile(profileId, userId))
                .thenReturn(List.of(credential));
        when(profileMapper.toResponse(profile, null, null)).thenReturn(profileResponse);

        when(profileRepository.findForReview(null, null, org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(profile)));

        var reviewCases = service.getAdminReviewCases(null, null, org.springframework.data.domain.PageRequest.of(0, 10), userId);

        assertThat(reviewCases.getContent()).hasSize(1);
        assertThat(reviewCases.getContent().get(0).getProfile().getExpertProfileId()).isEqualTo(profileId);
        assertThat(reviewCases.getContent().get(0).getCredentials()).containsExactly(credential);
        assertThat(reviewCases.getContent().get(0).getIdentityStatus()).isEqualTo("MISSING");
        assertThat(reviewCases.getContent().get(0).getCredentialStatus()).isEqualTo("APPROVED");
        assertThat(reviewCases.getContent().get(0).isReadyForFinalApproval()).isFalse();
    }

    private ExpertProfile profile() {
        return ExpertProfile.builder()
                .expertProfileId(profileId)
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
