package com.carebridge.backend.baby;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.dto.ArchiveBabyProfileResponse;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.baby.service.impl.BabyServiceImpl;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.vaccination.service.IVaccinationBookService;
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
 * UC33 — ArchiveBabyProfile service unit tests.
 * RED gate: all fail with UnsupportedOperationException until GREEN phase.
 */
@ExtendWith(MockitoExtension.class)
class BabyServiceArchiveTest {

    @Mock private BabyProfileRepository babyRepository;
    @Mock private BabyAccessPolicy accessPolicy;
    @Mock private AuditService auditService;
    @Mock private IVaccinationBookService vaccinationBookService;
    @InjectMocks private BabyServiceImpl babyService;

    /** BABY-TC-033-001: Happy path — status changes to ARCHIVED; audit emitted. */
    @Test
    void archiveBabyProfile_validRequest_returnsArchivedResponse() {
        var baby = BabyProfileArchiveTestFactory.makeActiveBaby();
        when(babyRepository.findById(BabyProfileArchiveTestFactory.BABY_ID)).thenReturn(Optional.of(baby));
        when(babyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArchiveBabyProfileResponse response = babyService.archiveBabyProfile(
                BabyProfileArchiveTestFactory.BABY_ID, BabyProfileArchiveTestFactory.MOTHER_ID);

        assertThat(response.getBabyId()).isEqualTo(BabyProfileArchiveTestFactory.BABY_ID);
        assertThat(response.getStatus()).isEqualTo(BabyProfileStatus.ARCHIVED.name());
        assertThat(response.getArchivedAt()).isNotNull();
        verify(auditService).log(
                eq(AuditAction.BABY_PROFILE_ARCHIVED),
                eq(BabyProfileArchiveTestFactory.MOTHER_ID),
                eq("BabyProfile"),
                any(),
                any());
        verify(babyRepository).save(any());
    }

    /** BABY-TC-033-002: CRITICAL — Cannot re-archive already archived baby → BABY-022 (400). NOT idempotent. */
    @Test
    void archiveBabyProfile_alreadyArchived_throwsBaby022() {
        var baby = BabyProfileArchiveTestFactory.makeArchivedBaby();
        when(babyRepository.findById(BabyProfileArchiveTestFactory.ARCHIVED_BABY_ID))
                .thenReturn(Optional.of(baby));

        assertThatThrownBy(() -> babyService.archiveBabyProfile(
                BabyProfileArchiveTestFactory.ARCHIVED_BABY_ID, BabyProfileArchiveTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BABY-022");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verify(babyRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    /** BABY-TC-033-003: CRITICAL SECURITY — IDOR: other user's baby → BABY-021 (403). */
    @Test
    void archiveBabyProfile_idor_throwsBaby021() {
        var baby = BabyProfileArchiveTestFactory.makeOtherMotherBaby(); // owned by OTHER_MOTHER_ID
        when(babyRepository.findById(BabyProfileArchiveTestFactory.OTHER_BABY_ID))
                .thenReturn(Optional.of(baby));

        assertThatThrownBy(() -> babyService.archiveBabyProfile(
                BabyProfileArchiveTestFactory.OTHER_BABY_ID, BabyProfileArchiveTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BABY-021");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        verify(babyRepository, never()).save(any());
    }

    /** BABY-TC-033-004: Baby not found → BABY-020 (404). */
    @Test
    void archiveBabyProfile_notFound_throwsBaby020() {
        when(babyRepository.findById(BabyProfileArchiveTestFactory.UNKNOWN_BABY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> babyService.archiveBabyProfile(
                BabyProfileArchiveTestFactory.UNKNOWN_BABY_ID, BabyProfileArchiveTestFactory.MOTHER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("BABY-020");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(babyRepository, never()).save(any());
    }
}
