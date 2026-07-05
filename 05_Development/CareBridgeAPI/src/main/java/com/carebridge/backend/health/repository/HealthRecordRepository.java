package com.carebridge.backend.health.repository;

import com.carebridge.backend.health.entity.HealthRecord;
import com.carebridge.backend.health.entity.HealthRecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {

    Optional<HealthRecord> findByIdAndStatus(UUID id, HealthRecordStatus status);

    @Query("""
            SELECT r FROM HealthRecord r
            WHERE r.ownerUserId = :ownerUserId
              AND r.status = 'ACTIVE'
              AND (:recordType IS NULL OR CAST(r.recordType AS string) = :recordType)
              AND (:journeyId IS NULL OR r.journeyId = :journeyId)
              AND (:babyId IS NULL OR r.babyId = :babyId)
              AND (:sourceType IS NULL OR r.sourceType = :sourceType)
            ORDER BY r.recordDate DESC, r.createdAt DESC
            """)
    Page<HealthRecord> findActiveByOwnerFiltered(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("recordType") String recordType,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId,
            @Param("sourceType") String sourceType,
            Pageable pageable);
}
