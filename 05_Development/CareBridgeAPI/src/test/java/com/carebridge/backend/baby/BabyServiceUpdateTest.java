package com.carebridge.backend.baby;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.dto.UpdateBabyProfileResponse;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC32 — UpdateBabyProfile service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class BabyServiceUpdateTest {

    @Mock private BabyProfileRepository babyRepository;
    @Mock private BabyAccessPolicy accessPolicy;
    @Mock private AuditService auditService;
    @InjectMocks private BabyServiceImpl babyService;

    /** BABY-TC-032-001: Happy path — update all mutable fields; status remains ACTIVE. */
    @Test
    void updateBabyProfile_validRequest_returnsUpdatedResponse() {
        var baby = BabyProfileUpdateTestFactory.makeActiveBaby();
        var req = BabyProfileUpdateTestFactory.makeUpdateRequest();
        when(babyRepository.findById(BabyProfileUpdateTestFactory.BABY_ID)).thenReturn(Optional.of(baby));
        when(babyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBabyProfileResponse response = babyService.updateBabyProfile(
                BabyProfileUpdateTestFactory.BABY_ID, req, BabyProfileUpdateTestFactory.MOTHER_ID);

        assertThat(response.getBabyId()).isEqualTo(BabyProfileUpdateTestFactory.BABY_ID);
        assertThat(response.getNickname()).isEqualTo("Updated Bean");
        assertThat(response.getStatus()).isEqualTo(BabyProfileStatus.ACTIVE.name());
        verify(auditService).log(
                eq(AuditAction.BABY_PROFILE_UPDATED),
                eq(BabyProfileUpdateTestFactory.MOTHER_ID),
                eq("BabyProfile"),
                any(),
                any());
        verify(babyRepository).save(any());
    }

    /** BABY-TC-032-002: Baby not found → BABY-010 (404). */
    @Test
    void updateBabyProfile_notFound_throwsBaby010() {
        var req = BabyProfileUpdateTestFactory.makeUpdateRequest();
        when(babyRepository.findById(BabyProfileUpdateTestFactory.UNKNOWN_BABY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> babyService.updateBabyProfile(
                BabyProfileUpdateTestFactory.UNKNOWN_BABY_ID, req, BabyProfileUpdateTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BABY-010");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(babyRepository, never()).save(any());
    }

    /** BABY-TC-032-003: CRITICAL SECURITY — IDOR: other user's baby → BABY-011 (403). */
    @Test
    void updateBabyProfile_idor_throwsBaby011() {
        var baby = BabyProfileUpdateTestFactory.makeOtherMotherBaby(); // owned by OTHER_MOTHER_ID
        var req = BabyProfileUpdateTestFactory.makeUpdateRequest();
        when(babyRepository.findById(BabyProfileUpdateTestFactory.OTHER_BABY_ID))
                .thenReturn(Optional.of(baby));

        assertThatThrownBy(() -> babyService.updateBabyProfile(
                BabyProfileUpdateTestFactory.OTHER_BABY_ID, req, BabyProfileUpdateTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BABY-011");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(babyRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** BABY-TC-032-004: Cannot update ARCHIVED baby → BABY-012 (400). */
    @Test
    void updateBabyProfile_archived_throwsBaby012() {
        var baby = BabyProfileUpdateTestFactory.makeArchivedBaby();
        var req = BabyProfileUpdateTestFactory.makeUpdateRequest();
        when(babyRepository.findById(BabyProfileUpdateTestFactory.ARCHIVED_BABY_ID))
                .thenReturn(Optional.of(baby));

        assertThatThrownBy(() -> babyService.updateBabyProfile(
                BabyProfileUpdateTestFactory.ARCHIVED_BABY_ID, req, BabyProfileUpdateTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BABY-012");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(babyRepository, never()).save(any());
    }
}
