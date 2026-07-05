package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GrowthMeasurementRepository extends JpaRepository<GrowthMeasurement, UUID> {

    List<GrowthMeasurement> findByBabyIdOrderByMeasuredDateAsc(UUID babyId);
}
