package com.carebridge.backend.vaccination.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.vaccination.config.VaccinationProperties;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.entity.VaccinationReferenceSchedule;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.carebridge.backend.vaccination.repository.VaccinationReferenceRepository;
import com.carebridge.backend.vaccination.service.IVaccinationBookService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VaccinationBookService implements IVaccinationBookService {

    private static final Logger log = LoggerFactory.getLogger(VaccinationBookService.class);

    private final VaccinationReferenceRepository referenceRepository;
    private final VaccinationRecordRepository recordRepository;
    private final VaccinationProperties properties;
    private final AuditService auditService;

    @Override
    @Transactional
    public int initializeBook(BabyProfile baby) {
        if (baby == null || baby.getBirthDate() == null) {
            return 0;
        }

        List<VaccinationReferenceSchedule> catalogue = activeCatalogue();
        if (catalogue.isEmpty()) {
            log.warn("Vaccination catalogue '{}' is empty; no book materialised for baby {}",
                    properties.getScheduleVersion(), baby.getId());
            return 0;
        }

        Set<String> existing = existingDoseKeys(baby);
        List<VaccinationRecord> created = new ArrayList<>();
        for (VaccinationReferenceSchedule dose : catalogue) {
            if (existing.contains(doseKey(dose.getVaccineName(), dose.getDoseNumber()))) {
                continue;
            }
            created.add(materialise(baby, dose));
        }
        if (created.isEmpty()) {
            return 0;
        }

        recordRepository.saveAll(created);
        auditService.log(AuditAction.VACCINATION_BOOK_INITIALIZED, baby.getOwnerUserId(),
                "BabyProfile", baby.getId().toString(),
                "Materialised " + created.size() + " expected doses from catalogue "
                        + properties.getScheduleVersion());
        return created.size();
    }

    @Override
    @Transactional
    public int realignBook(BabyProfile baby) {
        if (baby == null || baby.getBirthDate() == null) {
            return 0;
        }

        List<VaccinationReferenceSchedule> catalogue = activeCatalogue();
        if (catalogue.isEmpty()) {
            return 0;
        }

        Map<String, VaccinationRecord> byDose = new HashMap<>();
        for (VaccinationRecord record : recordRepository.findAllByBabyId(baby.getId())) {
            byDose.putIfAbsent(doseKey(record.getVaccineName(), record.getDoseNumber()), record);
        }

        List<VaccinationRecord> touched = new ArrayList<>();
        for (VaccinationReferenceSchedule dose : catalogue) {
            LocalDate expected = baby.getBirthDate().plusDays(dose.getOffsetDays());
            VaccinationRecord record = byDose.get(doseKey(dose.getVaccineName(), dose.getDoseNumber()));
            if (record == null) {
                touched.add(materialise(baby, dose));
                continue;
            }
            // Only the untouched part of the book follows the birth date. A dose the mother
            // completed, postponed or deleted keeps the date she chose.
            if (record.getStatus() == VaccinationRecordStatus.SCHEDULED
                    && !expected.equals(record.getScheduledDate())) {
                record.setScheduledDate(expected);
                record.setVaccinationScheduleId(dose.getId());
                touched.add(record);
            }
        }
        if (touched.isEmpty()) {
            return 0;
        }

        recordRepository.saveAll(touched);
        auditService.log(AuditAction.VACCINATION_BOOK_REALIGNED, baby.getOwnerUserId(),
                "BabyProfile", baby.getId().toString(),
                "Realigned " + touched.size() + " expected doses to birth date " + baby.getBirthDate());
        return touched.size();
    }

    private List<VaccinationReferenceSchedule> activeCatalogue() {
        return referenceRepository
                .findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc(
                        properties.getScheduleVersion());
    }

    private Set<String> existingDoseKeys(BabyProfile baby) {
        Set<String> keys = new HashSet<>();
        for (VaccinationRecord record : recordRepository.findAllByBabyId(baby.getId())) {
            keys.add(doseKey(record.getVaccineName(), record.getDoseNumber()));
        }
        return keys;
    }

    private VaccinationRecord materialise(BabyProfile baby, VaccinationReferenceSchedule dose) {
        return VaccinationRecord.builder()
                .babyId(baby.getId())
                .careSubjectId(baby.getId())
                .vaccinationScheduleId(dose.getId())
                .vaccineName(dose.getVaccineName())
                .doseNumber(dose.getDoseNumber())
                .scheduledDate(baby.getBirthDate().plusDays(dose.getOffsetDays()))
                .status(VaccinationRecordStatus.SCHEDULED)
                .build();
    }

    private String doseKey(String vaccineName, Short doseNumber) {
        return vaccineName + "|" + doseNumber;
    }
}
