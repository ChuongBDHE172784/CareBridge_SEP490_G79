package com.carebridge.backend.vaccination.repository;

import com.carebridge.backend.vaccination.entity.VaccinationReferenceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VaccinationReferenceRepository extends JpaRepository<VaccinationReferenceSchedule, UUID> {

    List<VaccinationReferenceSchedule> findAllByOrderByOffsetDaysAsc();
}
