package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.HealthSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthSummaryRepository extends JpaRepository<HealthSummary, UUID> {

    Optional<HealthSummary> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    @Query("""
            SELECT s FROM HealthSummary s
            WHERE s.ownerUserId = :ownerUserId
              AND s.status = 'ACTIVE'
              AND (:summaryPeriod IS NULL OR s.summaryPeriod = :summaryPeriod)
              AND (:from IS NULL OR s.periodStart >= :from)
              AND (:to IS NULL OR s.periodEnd <= :to)
            ORDER BY s.createdAt DESC
            """)
    List<HealthSummary> findActiveByOwnerFiltered(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("summaryPeriod") String summaryPeriod,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
