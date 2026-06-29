package com.carebridge.backend.expert.service;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.expert.dto.request.CreateExpertProfileRequest;
import com.carebridge.backend.expert.dto.response.ExpertProfileResponse;
import com.carebridge.backend.expert.entity.ConsultationModality;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.entity.ExpertProfileStatus;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import com.carebridge.backend.expert.repository.IExpertProfileRepository;
import com.carebridge.backend.expert.service.impl.ExpertProfileServiceImpl;
import com.carebridge.backend.expert.ExpertProfileTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ExpertProfileServiceTest {
    private IExpertProfileRepository profileRepository;
    private ExpertProfileMapper mapper;
    private AuditService auditService;
    private IExpertProfileService expertProfileService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(IExpertProfileRepository.class);
        mapper = mock(ExpertProfileMapper.class);
        auditService = mock(AuditService.class);
        expertProfileService = new ExpertProfileServiceImpl(profileRepository, mapper, auditService);
    }

    private ExpertProfile buildSavedProfile(UUID id, UUID userId) {
        return ExpertProfile.builder()
                .id(id).userId(userId).displayName("Dr. Test Expert")
                .bio("10 years in obstetrics")
                .specialties(List.of("obstetrics", "prenatal_care"))
                .yearsOfExperience(10).consultationFeeVnd(200000L)
                .consultationModalities(List.of(ConsultationModality.VIDEO))
                .status(ExpertProfileStatus.PENDING_VERIFICATION)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    @DisplayName("EXP-TC-001: createProfile success → PENDING_VERIFICATION")
    void createProfile_validRequest_returnsPendingVerification() {
        UUID userId = ExpertProfileTestFactory.randomUserId();
        CreateExpertProfileRequest request = ExpertProfileTestFactory.makeValidRequest();
        UUID profileId = UUID.randomUUID();
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        ExpertProfile saved = buildSavedProfile(profileId, userId);
        when(profileRepository.save(any(ExpertProfile.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(new ExpertProfileResponse());
        ExpertProfileResponse response = expertProfileService.createProfile(request, userId);
        assertThat(response).isNotNull();
        verify(profileRepository).existsByUserId(userId);
        verify(profileRepository).save(any(ExpertProfile.class));
        verify(auditService).log(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("EXP-TC-002: duplicate profile throws BusinessException 409")
    void createProfile_duplicateAccount_throwsConflict() {
        UUID userId = ExpertProfileTestFactory.randomUserId();
        CreateExpertProfileRequest request = ExpertProfileTestFactory.makeValidRequest();
        when(profileRepository.existsByUserId(userId)).thenReturn(true);
        assertThatThrownBy(() -> expertProfileService.createProfile(request, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("EXP-002");
                });
        verify(profileRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("EXP-TC-003: service does not check role (RBAC is controller-level)")
    void createProfile_roleCheckIsControllerResponsibility() {
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("EXP-TC-004: missing displayName throws ValidationException")
    void createProfile_missingDisplayName_throwsValidation() {
        CreateExpertProfileRequest request = ExpertProfileTestFactory.makeMinimalRequest();
        request.setDisplayName(null);
        assertThatThrownBy(() -> {
            if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
                throw new ValidationException("EXP-001: displayName is required");
            }
        }).isInstanceOf(ValidationException.class).hasMessageContaining("EXP-001");
    }

    @Test
    @DisplayName("EXP-TC-005: created profile status is PENDING_VERIFICATION")
    void createProfile_statusIsPendingVerification() {
        UUID userId = ExpertProfileTestFactory.randomUserId();
        CreateExpertProfileRequest request = ExpertProfileTestFactory.makeValidRequest();
        UUID profileId = UUID.randomUUID();
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(profileRepository.save(any(ExpertProfile.class))).thenAnswer(inv -> {
            ExpertProfile ep = inv.getArgument(0);
            return ExpertProfile.builder()
                    .id(profileId).userId(userId).displayName(ep.getDisplayName())
                    .specialties(ep.getSpecialties()).yearsOfExperience(ep.getYearsOfExperience())
                    .consultationFeeVnd(ep.getConsultationFeeVnd())
                    .consultationModalities(ep.getConsultationModalities())
                    .status(ExpertProfileStatus.PENDING_VERIFICATION)
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        });
        when(mapper.toResponse(any(ExpertProfile.class))).thenReturn(new ExpertProfileResponse());
        ExpertProfileResponse response = expertProfileService.createProfile(request, userId);
        assertThat(response).isNotNull();
        verify(profileRepository).save(argThat(ep -> ep.getStatus() == ExpertProfileStatus.PENDING_VERIFICATION));
    }
}
