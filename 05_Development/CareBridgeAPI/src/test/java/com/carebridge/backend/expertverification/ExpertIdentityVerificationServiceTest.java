package com.carebridge.backend.expertverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertverification.adapter.FaceVerificationAdapter;
import com.carebridge.backend.expertverification.adapter.FaceVerificationResult;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.FaceVerificationStatus;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.service.impl.ExpertIdentityVerificationServiceImpl;
import com.carebridge.backend.file.dto.UploadFileResponse;
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

@ExtendWith(MockitoExtension.class)
class ExpertIdentityVerificationServiceTest {

    @Mock private ExpertProfileRepository profileRepository;
    @Mock private ExpertIdentityVerificationRepository identityRepository;
    @Mock private ExpertCredentialRepository credentialRepository;
    @Mock private FaceVerificationAdapter faceVerificationAdapter;
    @Mock private IFileService fileService;
    @Mock private AuditService auditService;

    private ExpertIdentityVerificationServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private final UUID profileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExpertIdentityVerificationServiceImpl(
                profileRepository, identityRepository, credentialRepository,
                faceVerificationAdapter, fileService, auditService);
    }

    @Test
    void disabledCompreFaceStoresAllEvidenceForManualReview() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile()));
        when(faceVerificationAdapter.verify(any(), any(), any(), any()))
                .thenReturn(new FaceVerificationResult(
                        FaceVerificationStatus.DISABLED, null, BigDecimal.valueOf(.75),
                        "PENDING_LINUX_VERIFICATION"));
        when(fileService.uploadFile(any(), eq(userId)))
                .thenReturn(upload(), upload(), upload());
        when(identityRepository.save(any())).thenAnswer(invocation -> {
            ExpertIdentityVerification value = invocation.getArgument(0);
            value.setId(UUID.randomUUID());
            return value;
        });

        var response = service.submit(userId, image("selfie"), image("front"), image("back"));

        assertThat(response.getReviewStatus()).isEqualTo(IdentityReviewStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(response.getFaceStatus()).isEqualTo(FaceVerificationStatus.DISABLED);
        verify(fileService, times(3)).uploadFile(any(), eq(userId));
        verify(identityRepository).save(any());
    }

    @Test
    void missingImageRejectsBeforeFaceOrStorageCalls() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile()));

        assertThatThrownBy(() -> service.submit(userId, null, image("front"), image("back")))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("EXPIDENT-001"));

        verifyNoInteractions(faceVerificationAdapter, fileService, identityRepository);
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
