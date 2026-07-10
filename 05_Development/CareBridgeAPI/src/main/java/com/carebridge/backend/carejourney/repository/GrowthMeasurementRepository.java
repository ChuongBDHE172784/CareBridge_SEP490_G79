package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GrowthMeasurementRepository extends JpaRepository<GrowthMeasurement, UUID> {

    List<GrowthMeasurement> findByBabyIdOrderByMeasuredDateAsc(UUID babyId);

    List<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(UUID babyId);

    Page<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(UUID babyId, Pageable pageable);

    Optional<GrowthMeasurement> findByGrowthMeasurementIdAndBabyId(UUID growthMeasurementId, UUID babyId);
}
