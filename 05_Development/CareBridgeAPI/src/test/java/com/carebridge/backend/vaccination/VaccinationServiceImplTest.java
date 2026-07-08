package com.carebridge.backend.vaccination;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.vaccination.dto.VaccinationScheduleResponse;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.entity.VaccinationReferenceSchedule;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.carebridge.backend.vaccination.repository.VaccinationReferenceRepository;
import com.carebridge.backend.vaccination.service.impl.VaccinationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaccinationServiceImplTest {

    @Mock private BabyProfileRepository babyRepository;
    @Mock private BabyAccessPolicy accessPolicy;
    @Mock private VaccinationReferenceRepository referenceRepository;
    @Mock private VaccinationRecordRepository recordRepository;
    @InjectMocks private VaccinationServiceImpl vaccinationService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BABY_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private BabyProfile makeBaby(LocalDate birthDate) {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(CALLER_ID)
                .nickname("Bean")
                .birthDate(birthDate)
                .status(BabyProfileStatus.ACTIVE)
                .build();
    }

    private VaccinationReferenceSchedule makeRef(String name, int dose, int offsetDays) {
        return VaccinationReferenceSchedule.builder()
                .id(UUID.randomUUID())
                .vaccineName(name)
                .doseNumber((short) dose)
                .offsetDays(offsetDays)
                .build();
    }

    // VAC-TC-001: Happy path — schedule computed from birthDate + offsetDays
    @Test
    void getVaccinationSchedule_validBaby_returnsDoses() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(61));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.findAllByOrderByOffsetDaysAsc())
                .thenReturn(List.of(makeRef("BCG", 1, 0), makeRef("DTP-VGB-Hib", 1, 60)));
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of());

        VaccinationScheduleResponse resp = vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID);

        assertThat(resp.getBabyId()).isEqualTo(BABY_ID);
        assertThat(resp.getDoses()).hasSize(2);
    }

    // VAC-TC-002: C2 — OVERDUE computed at query time (not stored in DB)
    @Test
    void getVaccinationSchedule_overdueNotYetAdministered_statusIsOVERDUE() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(70));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.findAllByOrderByOffsetDaysAsc())
                .thenReturn(List.of(makeRef("BCG", 1, 0)));
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of());

        VaccinationScheduleResponse resp = vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID);

        assertThat(resp.getDoses().get(0).getStatus()).isEqualTo("OVERDUE");
    }

    // VAC-TC-003: COMPLETED record → status COMPLETED
    @Test
    void getVaccinationSchedule_completedRecord_statusIsCOMPLETED() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        VaccinationReferenceSchedule ref = makeRef("BCG", 1, 0);
        when(referenceRepository.findAllByOrderByOffsetDaysAsc()).thenReturn(List.of(ref));

        VaccinationRecord completedRecord = VaccinationRecord.builder()
                .id(UUID.randomUUID()).babyId(BABY_ID)
                .vaccineName("BCG").doseNumber((short) 1)
                .administeredDate(LocalDate.now().minusDays(4))
                .status(VaccinationRecordStatus.COMPLETED)
                .build();
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of(completedRecord));

        VaccinationScheduleResponse resp = vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID);

        assertThat(resp.getDoses().get(0).getStatus()).isEqualTo("COMPLETED");
    }

    // VAC-TC-004: C3 — no medical recommendations in response
    @Test
    void getVaccinationSchedule_noRecommendationsInResponse() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.findAllByOrderByOffsetDaysAsc()).thenReturn(List.of());
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of());

        VaccinationScheduleResponse resp = vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID);

        assertThat(resp.toString()).doesNotContainIgnoringCase("recommend");
        assertThat(resp.toString()).doesNotContainIgnoringCase("prescribe");
    }

    // VAC-TC-005: Baby not found → VAC-001 / 404
    @Test
    void getVaccinationSchedule_babyNotFound_throwsBusinessException404() {
        when(babyRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("VAC-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // VAC-TC-006: C4 — BabyAccessPolicy called (no access → 403)
    @Test
    void getVaccinationSchedule_noAccess_throwsBusinessException403() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
