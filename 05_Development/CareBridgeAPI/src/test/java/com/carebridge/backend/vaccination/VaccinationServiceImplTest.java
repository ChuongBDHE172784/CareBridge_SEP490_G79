package com.carebridge.backend.vaccination;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.vaccination.config.VaccinationProperties;
import com.carebridge.backend.vaccination.dto.AddVaccinationRecordRequest;
import com.carebridge.backend.vaccination.dto.MarkVaccinationCompletedRequest;
import com.carebridge.backend.vaccination.dto.PostponeVaccinationRequest;
import com.carebridge.backend.vaccination.dto.PostponeVaccinationResponse;
import com.carebridge.backend.vaccination.dto.UpdateVaccinationRecordRequest;
import com.carebridge.backend.vaccination.dto.VaccinationRecordResponse;
import com.carebridge.backend.vaccination.dto.VaccinationCompletionResponse;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashSet;
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
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private AuditService auditService;
    @Spy private VaccinationProperties properties = new VaccinationProperties();
    @InjectMocks private VaccinationServiceImpl vaccinationService;

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BABY_ID   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_CALLER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_BABY_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RECORD_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PROOF_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

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

    private VaccinationRecord makeRecord(VaccinationRecordStatus status) {
        return VaccinationRecord.builder()
                .id(RECORD_ID)
                .babyId(BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .scheduledDate(LocalDate.now().plusDays(7))
                .administeredDate(status == VaccinationRecordStatus.COMPLETED ? LocalDate.now().minusDays(1) : null)
                .status(status)
                .facilityName("Original facility")
                .proofRecordId(null)
                .build();
    }

    private HealthRecord makeProof(UUID babyId) {
        return HealthRecord.builder()
                .id(PROOF_ID)
                .babyId(babyId)
                .ownerUserId(CALLER_ID)
                .title("Synthetic vaccination proof")
                .status(HealthRecordStatus.ACTIVE)
                .build();
    }

    private void arrangeOwner() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(30));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, CALLER_ID)).thenReturn(true);
    }

    private void assertBusinessCode(Throwable ex, String code, HttpStatus status) {
        BusinessException be = (BusinessException) ex;
        assertThat(be.getCode()).isEqualTo(code);
        assertThat(be.getHttpStatus()).isEqualTo(status);
    }

    private AddVaccinationRecordRequest addRequest() {
        AddVaccinationRecordRequest request = new AddVaccinationRecordRequest();
        request.setVaccineName("BCG");
        request.setDoseNumber((short) 1);
        request.setAdministeredDate(LocalDate.now().minusDays(1));
        request.setFacilityName("Synthetic clinic");
        return request;
    }

    private UpdateVaccinationRecordRequest updateRequest() {
        UpdateVaccinationRecordRequest request = new UpdateVaccinationRecordRequest();
        request.setFacilityName("Updated clinic");
        return request;
    }

    private MarkVaccinationCompletedRequest completionRequest() {
        MarkVaccinationCompletedRequest request = new MarkVaccinationCompletedRequest();
        request.setVaccineName("BCG");
        request.setDoseNumber((short) 1);
        request.setAdministeredDate(LocalDate.now().minusDays(1));
        request.setFacilityName("Synthetic clinic");
        return request;
    }

    private PostponeVaccinationRequest postponeRequest() {
        PostponeVaccinationRequest request = new PostponeVaccinationRequest();
        request.setVaccineName("BCG");
        request.setDoseNumber((short) 1);
        request.setNewScheduledDate(LocalDate.now().plusDays(14));
        request.setReason("Synthetic scheduling conflict");
        return request;
    }

    private VaccinationRecord assignIdIfMissing(VaccinationRecord record) {
        if (record.getId() == null) {
            record.setId(RECORD_ID);
        }
        return record;
    }

    // VAC-TC-001: Happy path — schedule computed from birthDate + offsetDays
    @Test
    void getVaccinationSchedule_validBaby_returnsDoses() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(61));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc("vn-2026"))
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
        when(referenceRepository.findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc("vn-2026"))
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
        when(referenceRepository.findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc("vn-2026")).thenReturn(List.of(ref));

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

    @Test
    void getVaccinationSchedule_deletedRecord_ignoresRecordAndComputesOverdue() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc("vn-2026")).thenReturn(List.of(makeRef("BCG", 1, 0)));
        VaccinationRecord deletedRecord = VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .administeredDate(LocalDate.now().minusDays(4))
                .status(VaccinationRecordStatus.DELETED)
                .build();
        when(recordRepository.findAllByBabyId(BABY_ID)).thenReturn(List.of(deletedRecord));

        VaccinationScheduleResponse resp = vaccinationService.getVaccinationSchedule(BABY_ID, CALLER_ID);

        assertThat(resp.getDoses().get(0).getStatus()).isEqualTo("OVERDUE");
        assertThat(resp.getDoses().get(0).getAdministeredDate()).isNull();
    }

    // VAC-TC-004: C3 — no medical recommendations in response
    @Test
    void getVaccinationSchedule_noRecommendationsInResponse() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.canView(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc("vn-2026")).thenReturn(List.of());
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

    @Test
    void addVaccinationRecord_futureAdministeredDate_throwsVac008() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, CALLER_ID)).thenReturn(true);

        AddVaccinationRecordRequest request = new AddVaccinationRecordRequest();
        request.setVaccineName("BCG");
        request.setDoseNumber((short) 1);
        request.setAdministeredDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("VAC-008");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                });
        verify(recordRepository, never()).save(any());
    }

    @Test
    void VAC229_TC_001_addVaccinationRecord_withoutProof_createsCompletedRecord() {
        arrangeOwner();
        AddVaccinationRecordRequest request = addRequest();
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumberAndStatus(
                BABY_ID, "BCG", (short) 1, VaccinationRecordStatus.COMPLETED)).thenReturn(Optional.empty());
        when(recordRepository.save(any(VaccinationRecord.class))).thenAnswer(invocation -> {
            VaccinationRecord saved = invocation.getArgument(0);
            saved.setId(RECORD_ID);
            return saved;
        });

        var response = vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request);

        assertThat(response.getId()).isEqualTo(RECORD_ID);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAdministeredDate()).isEqualTo(request.getAdministeredDate());
        verify(recordRepository).save(argThat(record ->
                record.getStatus() == VaccinationRecordStatus.COMPLETED
                        && record.getScheduledDate() == null
                        && record.getProofRecordId() == null));
    }

    @Test
    void VAC229_TC_002_addVaccinationRecord_withValidProof_persistsProofId() {
        arrangeOwner();
        AddVaccinationRecordRequest request = addRequest();
        request.setProofRecordId(PROOF_ID);
        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(makeProof(BABY_ID)));
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumberAndStatus(
                BABY_ID, "BCG", (short) 1, VaccinationRecordStatus.COMPLETED)).thenReturn(Optional.empty());
        when(recordRepository.save(any(VaccinationRecord.class)))
                .thenAnswer(invocation -> assignIdIfMissing(invocation.getArgument(0)));

        var response = vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request);

        assertThat(response.getProofRecordId()).isEqualTo(PROOF_ID);
    }

    @Test
    void VAC229_TC_003_004_addVaccinationRecord_babyNotFoundOrDenied() {
        AddVaccinationRecordRequest request = addRequest();
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-001", HttpStatus.NOT_FOUND));

        BabyProfile baby = makeBaby(LocalDate.now().minusDays(30));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, OTHER_CALLER_ID)).thenReturn(false);
        assertThatThrownBy(() -> vaccinationService.addVaccinationRecord(BABY_ID, OTHER_CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-002", HttpStatus.FORBIDDEN));
    }

    @Test
    void VAC229_TC_009_010_addVaccinationRecord_invalidProofRejected() {
        arrangeOwner();
        AddVaccinationRecordRequest request = addRequest();
        request.setProofRecordId(PROOF_ID);
        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-005", HttpStatus.NOT_FOUND));

        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(makeProof(OTHER_BABY_ID)));
        assertThatThrownBy(() -> vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-006", HttpStatus.FORBIDDEN));
    }

    @Test
    void VAC229_TC_011_addVaccinationRecord_duplicateCompletedRejected() {
        arrangeOwner();
        AddVaccinationRecordRequest request = addRequest();
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumberAndStatus(
                BABY_ID, "BCG", (short) 1, VaccinationRecordStatus.COMPLETED))
                .thenReturn(Optional.of(makeRecord(VaccinationRecordStatus.COMPLETED)));

        assertThatThrownBy(() -> vaccinationService.addVaccinationRecord(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-007", HttpStatus.CONFLICT));
        verify(recordRepository, never()).save(any());
    }

    @Test
    void VAC230_updateVaccinationRecord_contentProofStatusAndOwnershipRules() {
        arrangeOwner();
        VaccinationRecord record = makeRecord(VaccinationRecordStatus.COMPLETED);
        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID)).thenReturn(Optional.of(record));
        when(recordRepository.save(record)).thenReturn(record);

        UpdateVaccinationRecordRequest request = updateRequest();
        request.setVaccineName("DTP");
        request.setDoseNumber((short) 2);
        request.setAdministeredDate(LocalDate.now().minusDays(2));
        VaccinationRecordResponse response =
                vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, request);

        assertThat(response.getVaccineName()).isEqualTo("DTP");
        assertThat(response.getDoseNumber()).isEqualTo((short) 2);
        assertThat(response.getFacilityName()).isEqualTo("Updated clinic");
        assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED);
    }

    @Test
    void VAC230_updateVaccinationRecord_proofClearAndInvalidProofRules() {
        arrangeOwner();
        VaccinationRecord record = makeRecord(VaccinationRecordStatus.COMPLETED);
        record.setProofRecordId(UUID.randomUUID());
        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID)).thenReturn(Optional.of(record));
        when(recordRepository.save(record)).thenReturn(record);

        UpdateVaccinationRecordRequest clearRequest = new UpdateVaccinationRecordRequest();
        clearRequest.setClearProof(true);
        assertThat(vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, clearRequest)
                .getProofRecordId()).isNull();

        UpdateVaccinationRecordRequest proofRequest = new UpdateVaccinationRecordRequest();
        proofRequest.setProofRecordId(PROOF_ID);
        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(makeProof(OTHER_BABY_ID)));
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, proofRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-011", HttpStatus.CONFLICT));

        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, proofRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-010", HttpStatus.NOT_FOUND));
    }

    @Test
    void VAC230_updateVaccinationRecord_statusChangeDeniedBeforePartialApply() {
        arrangeOwner();
        VaccinationRecord record = makeRecord(VaccinationRecordStatus.COMPLETED);
        UpdateVaccinationRecordRequest request = updateRequest();
        request.getUnknownFields().add("status");

        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-012", HttpStatus.BAD_REQUEST));
        verifyNoInteractions(recordRepository);
        assertThat(record.getFacilityName()).isEqualTo("Original facility");
    }

    @Test
    void VAC230_updateVaccinationRecord_notFoundDeniedFutureAndDeletedRules() {
        UpdateVaccinationRecordRequest request = updateRequest();
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-001", HttpStatus.NOT_FOUND));

        BabyProfile baby = makeBaby(LocalDate.now().minusDays(30));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, OTHER_CALLER_ID)).thenReturn(false);
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, OTHER_CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-002", HttpStatus.FORBIDDEN));

        when(accessPolicy.isOwner(baby, CALLER_ID)).thenReturn(true);
        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-009", HttpStatus.NOT_FOUND));

        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID))
                .thenReturn(Optional.of(makeRecord(VaccinationRecordStatus.DELETED)));
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-009", HttpStatus.NOT_FOUND));

        UpdateVaccinationRecordRequest futureRequest = new UpdateVaccinationRecordRequest();
        futureRequest.setAdministeredDate(LocalDate.now().plusDays(1));
        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID))
                .thenReturn(Optional.of(makeRecord(VaccinationRecordStatus.COMPLETED)));
        assertThatThrownBy(() -> vaccinationService.updateVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID, futureRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-008", HttpStatus.UNPROCESSABLE_CONTENT));
    }

    @Test
    void VAC231_deleteVaccinationRecord_softDeleteIdempotentAndDeniedRules() {
        arrangeOwner();
        for (VaccinationRecordStatus status : List.of(
                VaccinationRecordStatus.COMPLETED,
                VaccinationRecordStatus.SCHEDULED,
                VaccinationRecordStatus.POSTPONED)) {
            VaccinationRecord record = makeRecord(status);
            when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID)).thenReturn(Optional.of(record));

            vaccinationService.deleteVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID);

            assertThat(record.getStatus()).isEqualTo(VaccinationRecordStatus.DELETED);
            verify(recordRepository).save(record);
        }

        VaccinationRecord deleted = makeRecord(VaccinationRecordStatus.DELETED);
        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID)).thenReturn(Optional.of(deleted));
        vaccinationService.deleteVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID);
        verify(recordRepository, never()).delete(any());
        verify(recordRepository, never()).deleteById(any());
    }

    @Test
    void VAC231_deleteVaccinationRecord_notFoundAndAccessRules() {
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.deleteVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-001", HttpStatus.NOT_FOUND));

        BabyProfile baby = makeBaby(LocalDate.now().minusDays(30));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, OTHER_CALLER_ID)).thenReturn(false);
        assertThatThrownBy(() -> vaccinationService.deleteVaccinationRecord(BABY_ID, RECORD_ID, OTHER_CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-013", HttpStatus.FORBIDDEN));

        when(accessPolicy.isOwner(baby, CALLER_ID)).thenReturn(true);
        when(recordRepository.findByIdAndBabyId(RECORD_ID, BABY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.deleteVaccinationRecord(BABY_ID, RECORD_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-014", HttpStatus.NOT_FOUND));
    }

    @Test
    void markVaccinationCompleted_existingPostponedRecord_updatesToCompleted() {
        BabyProfile baby = makeBaby(LocalDate.now().minusDays(5));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, CALLER_ID)).thenReturn(true);
        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(true);

        VaccinationRecord existing = VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .scheduledDate(LocalDate.now().plusDays(7))
                .status(VaccinationRecordStatus.POSTPONED)
                .build();
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.of(existing));
        when(recordRepository.save(existing)).thenReturn(existing);

        MarkVaccinationCompletedRequest request = new MarkVaccinationCompletedRequest();
        request.setVaccineName("BCG");
        request.setDoseNumber((short) 1);
        request.setAdministeredDate(LocalDate.now());

        VaccinationCompletionResponse response =
                vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, request);

        assertThat(response.isCreated()).isFalse();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(existing.getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED);
        assertThat(existing.getAdministeredDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void VAC232_markCompleted_updatesScheduledOrCreatesNewRecord() {
        arrangeOwner();
        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(true);
        VaccinationRecord scheduled = makeRecord(VaccinationRecordStatus.SCHEDULED);
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.of(scheduled));
        when(recordRepository.save(any(VaccinationRecord.class)))
                .thenAnswer(invocation -> assignIdIfMissing(invocation.getArgument(0)));

        VaccinationCompletionResponse updated =
                vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, completionRequest());

        assertThat(updated.isCreated()).isFalse();
        assertThat(scheduled.getStatus()).isEqualTo(VaccinationRecordStatus.COMPLETED);

        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.empty());
        VaccinationCompletionResponse created =
                vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, completionRequest());
        assertThat(created.isCreated()).isTrue();
        verify(recordRepository, times(2)).save(any(VaccinationRecord.class));
    }

    @Test
    void VAC232_markCompleted_rejectsCompletedReferenceProofFutureAndAccessRules() {
        MarkVaccinationCompletedRequest request = completionRequest();
        arrangeOwner();
        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(false);
        assertThatThrownBy(() -> vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-019", HttpStatus.NOT_FOUND));

        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(true);
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.of(makeRecord(VaccinationRecordStatus.COMPLETED)));
        assertThatThrownBy(() -> vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-018", HttpStatus.CONFLICT));

        request.setAdministeredDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-008", HttpStatus.UNPROCESSABLE_CONTENT));
    }

    @Test
    void VAC232_markCompleted_validAndForeignProofRules() {
        arrangeOwner();
        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(true);
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.empty());
        when(recordRepository.save(any(VaccinationRecord.class)))
                .thenAnswer(invocation -> assignIdIfMissing(invocation.getArgument(0)));
        MarkVaccinationCompletedRequest request = completionRequest();
        request.setProofRecordId(PROOF_ID);
        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(makeProof(BABY_ID)));
        assertThat(vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, request).getProofRecordId())
                .isEqualTo(PROOF_ID);

        when(healthRecordRepository.findByIdAndStatus(PROOF_ID, HealthRecordStatus.ACTIVE))
                .thenReturn(Optional.of(makeProof(OTHER_BABY_ID)));
        assertThatThrownBy(() -> vaccinationService.markVaccinationCompleted(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-020", HttpStatus.CONFLICT));
    }

    @Test
    void VAC233_postpone_updatesCreatesRepeatsAndRejectsCompleted() {
        arrangeOwner();
        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(true);
        VaccinationRecord scheduled = makeRecord(VaccinationRecordStatus.SCHEDULED);
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.of(scheduled));
        when(recordRepository.save(any(VaccinationRecord.class)))
                .thenAnswer(invocation -> assignIdIfMissing(invocation.getArgument(0)));

        PostponeVaccinationResponse updated =
                vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, postponeRequest());

        assertThat(updated.isCreated()).isFalse();
        assertThat(scheduled.getStatus()).isEqualTo(VaccinationRecordStatus.POSTPONED);
        assertThat(scheduled.getPostponeReason()).isEqualTo("Synthetic scheduling conflict");

        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.empty());
        assertThat(vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, postponeRequest()).isCreated()).isTrue();

        VaccinationRecord postponed = makeRecord(VaccinationRecordStatus.POSTPONED);
        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.of(postponed));
        assertThat(vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, postponeRequest()).isCreated()).isFalse();

        when(recordRepository.findByBabyIdAndVaccineNameAndDoseNumber(BABY_ID, "BCG", (short) 1))
                .thenReturn(Optional.of(makeRecord(VaccinationRecordStatus.COMPLETED)));
        assertThatThrownBy(() -> vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, postponeRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-023", HttpStatus.CONFLICT));
    }

    @Test
    void VAC233_postpone_referenceDateBabyAndOwnershipRules() {
        PostponeVaccinationRequest request = postponeRequest();
        arrangeOwner();
        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(false);
        assertThatThrownBy(() -> vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-022", HttpStatus.NOT_FOUND));

        when(referenceRepository.existsByVaccineNameAndDoseNumber("BCG", (short) 1)).thenReturn(true);
        request.setNewScheduledDate(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-024", HttpStatus.UNPROCESSABLE_CONTENT));

        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> vaccinationService.postponeVaccination(BABY_ID, CALLER_ID, postponeRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-001", HttpStatus.NOT_FOUND));

        BabyProfile baby = makeBaby(LocalDate.now().minusDays(30));
        when(babyRepository.findById(BABY_ID)).thenReturn(Optional.of(baby));
        when(accessPolicy.isOwner(baby, OTHER_CALLER_ID)).thenReturn(false);
        assertThatThrownBy(() -> vaccinationService.postponeVaccination(BABY_ID, OTHER_CALLER_ID, postponeRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertBusinessCode(ex, "VAC-002", HttpStatus.FORBIDDEN));
    }
}
