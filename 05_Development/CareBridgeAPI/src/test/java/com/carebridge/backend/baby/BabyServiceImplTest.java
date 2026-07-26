package com.carebridge.backend.baby;


import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.dto.BabyProfileDetailResponse;
import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.dto.CreateBabyProfileResponse;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.entity.Gender;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.baby.service.impl.BabyServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BabyServiceImplTest {

    @Mock private BabyProfileRepository babyRepository;
    @Mock private BabyAccessPolicy accessPolicy;
    @Mock private AuditService auditService;
    @InjectMocks private BabyServiceImpl babyService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private CreateBabyProfileRequest makeRequest() {
        CreateBabyProfileRequest req = new CreateBabyProfileRequest();
        req.setNickname("Baby Bean");
        req.setBirthDate(LocalDate.now().minusDays(10));
        req.setGender(Gender.FEMALE);
        req.setBirthWeightKg(new BigDecimal("3.2"));
        req.setBirthLengthCm(new BigDecimal("50.0"));
        return req;
    }

    private BabyProfile savedProfile(UUID id) {
        return BabyProfile.builder()
                .id(id)
                .ownerUserId(CALLER_ID)
                .nickname("Baby Bean")
                .birthDate(LocalDate.now().minusDays(10))
                .gender(Gender.FEMALE)
                .birthWeightKg(new BigDecimal("3.2"))
                .birthLengthCm(new BigDecimal("50.0"))
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    // BABY-TC-001: Happy path
    @Test
    void createBabyProfile_validRequest_returnsCreatedProfile() {
        BabyProfile saved = savedProfile(PROFILE_ID);
        when(babyRepository.save(any())).thenReturn(saved);

        CreateBabyProfileResponse resp = babyService.createBabyProfile(makeRequest(), CALLER_ID);

        assertThat(resp.getId()).isEqualTo(PROFILE_ID);
        assertThat(resp.getStatus()).isEqualTo("ACTIVE");
    }

    // BABY-TC-002: C5 — no medical diagnosis from birth measurements
    @Test
    void createBabyProfile_normalWeight_noDiagnosisInResponse() {
        when(babyRepository.save(any())).thenReturn(savedProfile(PROFILE_ID));

        CreateBabyProfileResponse resp = babyService.createBabyProfile(makeRequest(), CALLER_ID);

        assertThat(resp).doesNotHaveToString("diagnosis");
        assertThat(resp).doesNotHaveToString("condition");
    }

    // BABY-TC-003: accountId from JWT
    @Test
    void createBabyProfile_ownerSetToCallerId() {
        when(babyRepository.save(any())).thenReturn(savedProfile(PROFILE_ID));

        babyService.createBabyProfile(makeRequest(), CALLER_ID);

        verify(babyRepository).save(argThat(p -> p.getOwnerUserId().equals(CALLER_ID)));
    }

    // BABY-TC-004: View — owner can view own profile
    @Test
    void getBabyProfile_ownerAccess_returnsProfile() {
        BabyProfile profile = savedProfile(PROFILE_ID);
        when(babyRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(accessPolicy.canView(profile, CALLER_ID)).thenReturn(true);

        BabyProfileDetailResponse resp = babyService.getBabyProfile(PROFILE_ID, CALLER_ID);

        assertThat(resp.getId()).isEqualTo(PROFILE_ID);
    }

    @Test
    void getBabyProfile_delegatedViewerUsesViewerActiveSelection() {
        UUID ownerId = UUID.randomUUID();
        BabyProfile profile = savedProfile(PROFILE_ID);
        profile.setOwnerUserId(ownerId);
        when(babyRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(accessPolicy.canView(profile, CALLER_ID)).thenReturn(true);
        when(babyRepository.findActiveBabyId(CALLER_ID)).thenReturn(Optional.of(PROFILE_ID));

        BabyProfileDetailResponse response = babyService.getBabyProfile(PROFILE_ID, CALLER_ID);

        assertThat(response.getActive()).isTrue();
        verify(babyRepository).findActiveBabyId(CALLER_ID);
        verify(babyRepository, never()).findActiveBabyId(ownerId);
    }

    // BABY-TC-005: View — ARCHIVED profile still returns 200 (BR-BABY-011)
    @Test
    void getBabyProfile_archivedProfile_returns200NotException() {
        BabyProfile profile = savedProfile(PROFILE_ID);
        profile.setStatus(BabyProfileStatus.ARCHIVED);
        when(babyRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(accessPolicy.canView(profile, CALLER_ID)).thenReturn(true);

        BabyProfileDetailResponse resp = babyService.getBabyProfile(PROFILE_ID, CALLER_ID);

        assertThat(resp.getStatus()).isEqualTo("ARCHIVED");
    }

    // BABY-TC-006: View — unauthorized access → 403
    @Test
    void getBabyProfile_noAccess_throwsBusinessException403() {
        BabyProfile profile = savedProfile(PROFILE_ID);
        when(babyRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(accessPolicy.canView(profile, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> babyService.getBabyProfile(PROFILE_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // BABY-TC-007: View — profile not found → 404
    @Test
    void getBabyProfile_notFound_throwsBusinessException404() {
        when(babyRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> babyService.getBabyProfile(PROFILE_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void switchActiveBabyProfile_ownerSwitchesExactlyOneActiveProfile() {
        BabyProfile profile = savedProfile(PROFILE_ID);
        when(babyRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(accessPolicy.canManage(profile, CALLER_ID)).thenReturn(true);

        BabyProfileDetailResponse response = babyService.switchActiveBabyProfile(PROFILE_ID, CALLER_ID);

        assertThat(response.getActive()).isTrue();
        verify(babyRepository).setActiveBaby(CALLER_ID, PROFILE_ID);
        verify(babyRepository, never()).save(any());
        verify(auditService).log(any(), eq(CALLER_ID), eq("BabyProfile"), eq(PROFILE_ID.toString()), eq("active"));
    }
}
