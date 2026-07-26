package com.carebridge.backend.expert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.repository.ProfessionalSpecialtyRepository;
import com.carebridge.backend.expert.service.impl.ExpertProfileServiceImpl;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.entity.ExpertIdentityVerification;
import com.carebridge.backend.expertverification.enums.IdentityReviewStatus;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.repository.ExpertIdentityVerificationRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.masterdata.repository.SpecialtyRepository;
import com.carebridge.backend.map.repository.CareFacilityRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpertConsultationEligibilityTest {

    @Mock private ExpertProfileRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private CareFacilityRepository careFacilityRepository;
    @Mock private ProfessionalSpecialtyRepository professionalSpecialtyRepository;
    @Mock private ExpertIdentityVerificationRepository identityRepository;
    @Mock private ExpertCredentialRepository credentialRepository;
    @Mock private AuditService auditService;

    private final ExpertProfileMapper mapper = new ExpertProfileMapper();

    @Test
    void mapperExposesOnlyTheDerivedEligibilityBooleanOnBothPublicDtos() {
        ExpertProfile eligible = profile(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        eligible.setConsultationScope("Postpartum nutrition");
        ExpertProfile suspended = profile(VerificationStatus.APPROVED, TrustStatus.SUSPENDED);

        assertThat(mapper.toResponse(eligible, "Expert", null).isConsultationEligible())
                .isTrue();
        assertThat(mapper.toDetailResponse(eligible, "Expert", null)
                .isConsultationEligible()).isTrue();
        assertThat(mapper.toDetailResponse(eligible, "Expert", null)
                .getConsultationScope()).isEqualTo("Postpartum nutrition");
        assertThat(mapper.toResponse(suspended, "Expert", null).isConsultationEligible())
                .isFalse();
        assertThat(mapper.toDetailResponse(suspended, "Expert", null)
                .isConsultationEligible()).isFalse();
    }

    @Test
    void publicProfileIsHiddenWhenTrustIsNotActive() {
        ExpertProfile suspended = profile(VerificationStatus.APPROVED, TrustStatus.SUSPENDED);
        when(repository.findById(suspended.getExpertProfileId()))
                .thenReturn(Optional.of(suspended));
        ExpertProfileServiceImpl service =
                new ExpertProfileServiceImpl(repository, userRepository, mapper,
                        identityRepository, credentialRepository, auditService,
                        specialtyRepository, careFacilityRepository, professionalSpecialtyRepository);

        assertThatThrownBy(() -> service.getPublicProfile(suspended.getExpertProfileId()))
                .isInstanceOfSatisfying(ExpertException.class,
                        error -> assertThat(error.getCode()).isEqualTo("EXPERT-004"));
    }

    @Test
    void everyEligibilityMutationUsesTheSameExpertRowLock() {
        UUID profileId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ExpertProfile approve = profile(VerificationStatus.PENDING, TrustStatus.ACTIVE);
        approve.setExpertProfileId(profileId);
        ExpertProfile reject = profile(VerificationStatus.UNDER_REVIEW, TrustStatus.ACTIVE);
        reject.setExpertProfileId(profileId);
        ExpertProfile trust = profile(VerificationStatus.APPROVED, TrustStatus.ACTIVE);
        trust.setExpertProfileId(profileId);
        when(repository.findByIdForUpdate(profileId))
                .thenReturn(Optional.of(approve), Optional.of(reject), Optional.of(trust));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(identityRepository.findFirstByExpertProfileIdOrderByCreatedAtDesc(profileId))
                .thenReturn(Optional.of(ExpertIdentityVerification.builder()
                        .reviewStatus(IdentityReviewStatus.APPROVED).build()));
        when(credentialRepository.findByExpertProfileIdAndReviewStatus(profileId, ReviewStatus.APPROVED))
                .thenReturn(java.util.List.of(ExpertCredential.builder()
                        .credentialType("MEDICAL_LICENSE")
                        .reviewStatus(ReviewStatus.APPROVED)
                        .build()));
        ExpertProfileServiceImpl service =
                new ExpertProfileServiceImpl(repository, userRepository, mapper,
                        identityRepository, credentialRepository, auditService,
                        specialtyRepository, careFacilityRepository, professionalSpecialtyRepository);

        service.approveExpert(profileId, adminId);
        service.rejectExpert(profileId, adminId, "reason");
        service.setTrustStatus(profileId, TrustStatus.REVOKED, adminId);

        verify(repository, times(3)).findByIdForUpdate(profileId);
        verify(repository, never()).findById(profileId);
        verify(repository, times(3)).save(any());
    }

    private static ExpertProfile profile(
            VerificationStatus verificationStatus, TrustStatus trustStatus) {
        return ExpertProfile.builder()
                .expertProfileId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .verificationStatus(verificationStatus)
                .trustStatus(trustStatus)
                .build();
    }
}
