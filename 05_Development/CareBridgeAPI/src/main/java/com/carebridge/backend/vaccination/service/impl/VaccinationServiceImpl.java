package com.carebridge.backend.vaccination.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import com.carebridge.backend.health.repository.HealthRecordRepository;
import com.carebridge.backend.vaccination.dto.AddVaccinationRecordRequest;
import com.carebridge.backend.vaccination.dto.AddVaccinationRecordResponse;
import com.carebridge.backend.vaccination.dto.MarkVaccinationCompletedRequest;
import com.carebridge.backend.vaccination.dto.PostponeVaccinationRequest;
import com.carebridge.backend.vaccination.dto.PostponeVaccinationResponse;
import com.carebridge.backend.vaccination.dto.UpdateVaccinationRecordRequest;
import com.carebridge.backend.vaccination.dto.VaccinationCompletionResponse;
import com.carebridge.backend.vaccination.dto.VaccinationDoseDto;
import com.carebridge.backend.vaccination.dto.VaccinationRecordResponse;
import com.carebridge.backend.vaccination.dto.VaccinationScheduleResponse;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.entity.VaccinationReferenceSchedule;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.carebridge.backend.vaccination.repository.VaccinationReferenceRepository;
import com.carebridge.backend.vaccination.service.IVaccinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VaccinationServiceImpl implements IVaccinationService {

    private final BabyProfileRepository babyRepository;
    private final BabyAccessPolicy accessPolicy;
    private final VaccinationReferenceRepository referenceRepository;
    private final VaccinationRecordRepository recordRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AuditService auditService;

    @Override
    public VaccinationScheduleResponse getVaccinationSchedule(UUID babyProfileId, UUID callerId) {
        // VAC-001: baby not found → 404
        BabyProfile baby = babyRepository.findById(babyProfileId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "VAC-001",
                        "Baby profile not found: " + babyProfileId));

        // C4: BabyAccessPolicy — ownership OR ACCEPTED care group member
        if (!accessPolicy.canView(baby, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "VAC-002",
                    "Access denied to vaccination schedule");
        }

        LocalDate birthDate = baby.getBirthDate();
        List<VaccinationReferenceSchedule> refs = referenceRepository.findAllByOrderByOffsetDaysAsc();
        List<VaccinationRecord> records = recordRepository.findAllByBabyId(babyProfileId).stream()
                .filter(record -> record.getStatus() != VaccinationRecordStatus.DELETED)
                .toList();

        // Index completed/postponed records by "vaccineName|doseNumber"
        Map<String, VaccinationRecord> recordMap = records.stream()
                .collect(Collectors.toMap(
                        r -> r.getVaccineName() + "|" + r.getDoseNumber(),
                        r -> r,
                        (a, b) -> a));

        LocalDate today = LocalDate.now();

        // C1: schedule derived from DB reference + birthDate (ADR-VAC-001)
        // C2: OVERDUE computed at query time — NOT stored
        // C3: no recommendations in response (BR-SAFETY)
        List<VaccinationDoseDto> doses = refs.stream().map(ref -> {
            LocalDate expectedDate = birthDate != null
                    ? birthDate.plusDays(ref.getOffsetDays())
                    : null;

            String key = ref.getVaccineName() + "|" + ref.getDoseNumber();
            VaccinationRecord record = recordMap.get(key);

            String status;
            LocalDate administeredDate = null;
            if (record != null && record.getStatus() == VaccinationRecordStatus.COMPLETED) {
                status = "COMPLETED";
                administeredDate = record.getAdministeredDate();
            } else if (record != null && record.getStatus() == VaccinationRecordStatus.POSTPONED) {
                status = "POSTPONED";
            } else if (expectedDate != null && expectedDate.isBefore(today)) {
                // C2: OVERDUE = expectedDate < today AND no COMPLETED record
                status = "OVERDUE";
            } else {
                status = "SCHEDULED";
            }

            return VaccinationDoseDto.builder()
                    .vaccineName(ref.getVaccineName())
                    .doseNumber(ref.getDoseNumber())
                    .expectedDate(expectedDate)
                    .administeredDate(administeredDate)
                    .status(status)
                    .build();
        }).collect(Collectors.toList());

        return VaccinationScheduleResponse.builder()
                .babyId(baby.getId())
                .babyNickname(baby.getNickname())
                .doses(doses)
                .build();
    }

    @Override
    @Transactional
    public AddVaccinationRecordResponse addVaccinationRecord(UUID babyId, UUID callerId,
                                                             AddVaccinationRecordRequest request) {
        requireOwner(babyId, callerId, "VAC-001", "VAC-002");
        rejectFutureDate(request.getAdministeredDate(), "VAC-008");
        validateProof(request.getProofRecordId(), babyId, "VAC-005", "VAC-006", HttpStatus.FORBIDDEN);
        recordRepository.findByBabyIdAndVaccineNameAndDoseNumberAndStatus(
                babyId, request.getVaccineName(), request.getDoseNumber(), VaccinationRecordStatus.COMPLETED)
                .ifPresent(existing -> {
                    throw new BusinessException(HttpStatus.CONFLICT, "VAC-007",
                            "Vaccination record already exists");
                });

        VaccinationRecord record = VaccinationRecord.builder()
                .babyId(babyId)
                .vaccineName(request.getVaccineName())
                .doseNumber(request.getDoseNumber())
                .administeredDate(request.getAdministeredDate())
                .status(VaccinationRecordStatus.COMPLETED)
                .facilityName(request.getFacilityName())
                .proofRecordId(request.getProofRecordId())
                .build();

        VaccinationRecord saved = recordRepository.save(record);
        auditService.log(AuditAction.VACCINATION_RECORD_ADDED, callerId, "VACCINATION_RECORD",
                saved.getId().toString(), "Added vaccination record for baby " + babyId);
        return AddVaccinationRecordResponse.builder()
                .id(saved.getId())
                .babyId(saved.getBabyId())
                .vaccineName(saved.getVaccineName())
                .doseNumber(saved.getDoseNumber())
                .administeredDate(saved.getAdministeredDate())
                .status(saved.getStatus().name())
                .facilityName(saved.getFacilityName())
                .proofRecordId(saved.getProofRecordId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public VaccinationRecordResponse updateVaccinationRecord(UUID babyId, UUID recordId, UUID callerId,
                                                             UpdateVaccinationRecordRequest request) {
        requireOwner(babyId, callerId, "VAC-001", "VAC-002");
        if (request.attemptsStatusChange()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VAC-012",
                    "Status cannot be changed through this endpoint");
        }

        VaccinationRecord record = recordRepository.findByIdAndBabyId(recordId, babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "VAC-009",
                        "Vaccination record not found"));
        if (record.getStatus() == VaccinationRecordStatus.DELETED) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "VAC-009", "Vaccination record not found");
        }

        if (request.getAdministeredDate() != null) {
            rejectFutureDate(request.getAdministeredDate(), "VAC-008");
            record.setAdministeredDate(request.getAdministeredDate());
        }
        if (request.getVaccineName() != null) {
            record.setVaccineName(request.getVaccineName());
        }
        if (request.getDoseNumber() != null) {
            record.setDoseNumber(request.getDoseNumber());
        }
        if (request.getFacilityName() != null) {
            record.setFacilityName(request.getFacilityName());
        }
        if (request.isClearProof()) {
            record.setProofRecordId(null);
        } else if (request.getProofRecordId() != null) {
            validateProof(request.getProofRecordId(), babyId, "VAC-010", "VAC-011", HttpStatus.CONFLICT);
            record.setProofRecordId(request.getProofRecordId());
        }

        VaccinationRecord saved = recordRepository.save(record);
        auditService.log(AuditAction.VACCINATION_RECORD_UPDATED, callerId, "VACCINATION_RECORD",
                saved.getId().toString(), "Updated vaccination record");
        return toRecordResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVaccinationRecord(UUID babyId, UUID recordId, UUID callerId) {
        requireOwner(babyId, callerId, "VAC-001", "VAC-013");
        VaccinationRecord record = recordRepository.findByIdAndBabyId(recordId, babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "VAC-014",
                        "Vaccination record not found"));
        if (record.getStatus() == VaccinationRecordStatus.DELETED) {
            return;
        }

        record.setStatus(VaccinationRecordStatus.DELETED);
        recordRepository.save(record);
        auditService.log(AuditAction.VACCINATION_RECORD_DELETED, callerId, "VACCINATION_RECORD",
                record.getId().toString(), "Deleted vaccination record");
    }

    @Override
    @Transactional
    public VaccinationCompletionResponse markVaccinationCompleted(UUID babyId, UUID callerId,
                                                                  MarkVaccinationCompletedRequest request) {
        requireOwner(babyId, callerId, "VAC-001", "VAC-002");
        rejectFutureDate(request.getAdministeredDate(), "VAC-008");
        validateReference(request.getVaccineName(), request.getDoseNumber(), "VAC-019");
        validateProof(request.getProofRecordId(), babyId, "VAC-005", "VAC-020", HttpStatus.CONFLICT);

        VaccinationRecord record = recordRepository
                .findByBabyIdAndVaccineNameAndDoseNumber(babyId, request.getVaccineName(), request.getDoseNumber())
                .filter(existing -> existing.getStatus() != VaccinationRecordStatus.DELETED)
                .orElse(null);
        boolean created = record == null;
        if (record != null && record.getStatus() == VaccinationRecordStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.CONFLICT, "VAC-018",
                    "Vaccination dose is already completed");
        }
        if (record == null) {
            record = VaccinationRecord.builder()
                    .babyId(babyId)
                    .vaccineName(request.getVaccineName())
                    .doseNumber(request.getDoseNumber())
                    .build();
        }

        record.setAdministeredDate(request.getAdministeredDate());
        record.setFacilityName(request.getFacilityName());
        record.setProofRecordId(request.getProofRecordId());
        record.setStatus(VaccinationRecordStatus.COMPLETED);
        VaccinationRecord saved = recordRepository.save(record);
        auditService.log(AuditAction.VACCINATION_COMPLETED, callerId, "VACCINATION_RECORD",
                saved.getId().toString(), "Marked vaccination completed");
        return VaccinationCompletionResponse.builder()
                .vaccinationRecordId(saved.getId())
                .babyId(saved.getBabyId())
                .vaccineName(saved.getVaccineName())
                .doseNumber(saved.getDoseNumber())
                .administeredDate(saved.getAdministeredDate())
                .status(saved.getStatus().name())
                .facilityName(saved.getFacilityName())
                .proofRecordId(saved.getProofRecordId())
                .created(created)
                .build();
    }

    @Override
    @Transactional
    public PostponeVaccinationResponse postponeVaccination(UUID babyId, UUID callerId,
                                                           PostponeVaccinationRequest request) {
        requireOwner(babyId, callerId, "VAC-001", "VAC-002");
        validateReference(request.getVaccineName(), request.getDoseNumber(), "VAC-022");
        if (request.getNewScheduledDate().isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VAC-024",
                    "New scheduled date cannot be in the past");
        }

        VaccinationRecord record = recordRepository
                .findByBabyIdAndVaccineNameAndDoseNumber(babyId, request.getVaccineName(), request.getDoseNumber())
                .filter(existing -> existing.getStatus() != VaccinationRecordStatus.DELETED)
                .orElse(null);
        boolean created = record == null;
        if (record != null && record.getStatus() == VaccinationRecordStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.CONFLICT, "VAC-023",
                    "Completed vaccination cannot be postponed");
        }

        LocalDate previousScheduledDate = record == null ? null : record.getScheduledDate();
        if (record == null) {
            record = VaccinationRecord.builder()
                    .babyId(babyId)
                    .vaccineName(request.getVaccineName())
                    .doseNumber(request.getDoseNumber())
                    .build();
        }

        record.setScheduledDate(request.getNewScheduledDate());
        record.setStatus(VaccinationRecordStatus.POSTPONED);
        record.setPostponeReason(request.getReason());
        VaccinationRecord saved = recordRepository.save(record);
        auditService.log(AuditAction.VACCINATION_POSTPONED, callerId, "VACCINATION_RECORD",
                saved.getId().toString(), "Postponed vaccination");
        return PostponeVaccinationResponse.builder()
                .vaccinationRecordId(saved.getId())
                .babyId(saved.getBabyId())
                .vaccineName(saved.getVaccineName())
                .doseNumber(saved.getDoseNumber())
                .previousScheduledDate(previousScheduledDate)
                .newScheduledDate(saved.getScheduledDate())
                .status(saved.getStatus().name())
                .reason(saved.getPostponeReason())
                .created(created)
                .build();
    }

    private BabyProfile requireOwner(UUID babyId, UUID callerId, String notFoundCode, String forbiddenCode) {
        BabyProfile baby = babyRepository.findById(babyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, notFoundCode,
                        "Baby profile not found: " + babyId));
        if (!accessPolicy.isOwner(baby, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, forbiddenCode,
                    "Only the baby owner can manage vaccination records");
        }
        return baby;
    }

    private void validateProof(UUID proofRecordId, UUID babyId, String notFoundCode, String crossBabyCode,
                               HttpStatus crossBabyStatus) {
        if (proofRecordId == null) {
            return;
        }
        HealthRecord proof = healthRecordRepository.findByIdAndStatus(proofRecordId, HealthRecordStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, notFoundCode,
                        "Proof health record not found"));
        if (proof.getBabyId() == null || !proof.getBabyId().equals(babyId)) {
            throw new BusinessException(crossBabyStatus, crossBabyCode,
                    "Proof health record does not belong to the baby");
        }
    }

    private void rejectFutureDate(LocalDate date, String code) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, code,
                    "Administered date cannot be in the future");
        }
    }

    private void validateReference(String vaccineName, Short doseNumber, String code) {
        if (!referenceRepository.existsByVaccineNameAndDoseNumber(vaccineName, doseNumber)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, code,
                    "Vaccination reference dose not found");
        }
    }

    private VaccinationRecordResponse toRecordResponse(VaccinationRecord record) {
        return VaccinationRecordResponse.builder()
                .id(record.getId())
                .babyId(record.getBabyId())
                .vaccineName(record.getVaccineName())
                .doseNumber(record.getDoseNumber())
                .scheduledDate(record.getScheduledDate())
                .administeredDate(record.getAdministeredDate())
                .status(record.getStatus().name())
                .facilityName(record.getFacilityName())
                .proofRecordId(record.getProofRecordId())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
