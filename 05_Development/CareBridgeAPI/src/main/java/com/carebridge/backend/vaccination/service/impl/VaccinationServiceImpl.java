package com.carebridge.backend.vaccination.service.impl;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.policy.BabyAccessPolicy;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.vaccination.dto.VaccinationDoseDto;
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
        List<VaccinationRecord> records = recordRepository.findAllByBabyId(babyProfileId);

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
}
