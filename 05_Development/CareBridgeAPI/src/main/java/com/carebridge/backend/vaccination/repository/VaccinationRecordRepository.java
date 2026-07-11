package com.carebridge.backend.vaccination.repository;

import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VaccinationRecordRepository extends JpaRepository<VaccinationRecord, UUID> {

    List<VaccinationRecord> findAllByBabyId(UUID babyId);

    Optional<VaccinationRecord> findByBabyIdAndVaccineNameAndDoseNumberAndStatus(
            UUID babyId, String vaccineName, short doseNumber, VaccinationRecordStatus status);

    List<VaccinationRecord> findByBabyIdAndStatus(UUID babyId, VaccinationRecordStatus status);

    Optional<VaccinationRecord> findByIdAndBabyId(UUID id, UUID babyId);

    Optional<VaccinationRecord> findByBabyIdAndVaccineNameAndDoseNumber(
            UUID babyId, String vaccineName, short doseNumber);
}
