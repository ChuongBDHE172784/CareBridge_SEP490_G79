package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.GrowthMeasurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GrowthMeasurementRepository extends JpaRepository<GrowthMeasurement, UUID> {

    @Query("""
            select g from GrowthMeasurement g
            where g.babyId = :babyId
            order by g.measuredDate asc, g.createdAt asc, g.growthMeasurementId asc
            """)
    List<GrowthMeasurement> findByBabyIdOrderByMeasuredDateAsc(@Param("babyId") UUID babyId);

    @Query("""
            select g from GrowthMeasurement g
            where g.babyId = :babyId and g.deletedAt is null
            order by g.measuredDate asc, g.createdAt asc, g.growthMeasurementId asc
            """)
    List<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateAsc(@Param("babyId") UUID babyId);

    @Query("""
            select g from GrowthMeasurement g
            where g.babyId = :babyId and g.deletedAt is null
            order by g.measuredDate desc, g.createdAt desc, g.growthMeasurementId desc
            """)
    Page<GrowthMeasurement> findByBabyIdAndDeletedAtIsNullOrderByMeasuredDateDesc(
            @Param("babyId") UUID babyId, Pageable pageable);

    Optional<GrowthMeasurement> findByGrowthMeasurementIdAndBabyId(UUID growthMeasurementId, UUID babyId);
}
