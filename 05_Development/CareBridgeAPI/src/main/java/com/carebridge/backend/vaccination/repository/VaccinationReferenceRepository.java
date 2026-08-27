package com.carebridge.backend.vaccination.repository;

import com.carebridge.backend.vaccination.entity.VaccinationReferenceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VaccinationReferenceRepository extends JpaRepository<VaccinationReferenceSchedule, UUID> {

    List<VaccinationReferenceSchedule> findAllByOrderByOffsetDaysAsc();

    /**
     * The active catalogue for one schedule version, in the order a vaccination book is read:
     * earliest expected dose first, then dose number, then vaccine name so the ordering is
     * stable for doses that share an offset.
     */
    List<VaccinationReferenceSchedule> findByScheduleVersionOrderByOffsetDaysAscDoseNumberAscVaccineNameAsc(
            String scheduleVersion);

    boolean existsByVaccineNameAndDoseNumber(String vaccineName, short doseNumber);
}
