package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.BabyDailyLog;
import com.carebridge.backend.carejourney.entity.BabyDailyLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BabyDailyLogRepository extends JpaRepository<BabyDailyLog, UUID> {

    List<BabyDailyLog> findByBabyId(UUID babyId);

    List<BabyDailyLog> findByBabyIdAndStatusOrderByCreatedAtDesc(UUID babyId, BabyDailyLogStatus status);

    Optional<BabyDailyLog> findByBabyLogIdAndStatus(UUID babyLogId, BabyDailyLogStatus status);

    @Query(value = """
        SELECT log_type AS logType,
               COUNT(*) AS count,
               COALESCE(SUM(quantity), 0) AS totalQuantity,
               MAX(quantity) AS maxQuantity,
               MAX(unit) AS unit
        FROM care_logs
        WHERE care_subject_id = :babyId
          AND COALESCE(started_at, created_at) >= :fromDate
          AND COALESCE(started_at, created_at) < :toDate
          AND COALESCE(status, 'ACTIVE') = 'ACTIVE'
        GROUP BY log_type
        """, nativeQuery = true)
    List<LogTypeAggregateRow> aggregateByLogType(
        @Param("babyId") UUID babyId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query(value = """
        SELECT note FROM care_logs
        WHERE care_subject_id = :babyId
          AND log_type = :logType
          AND COALESCE(started_at, created_at) >= :fromDate
          AND COALESCE(started_at, created_at) < :toDate
          AND note IS NOT NULL
          AND COALESCE(status, 'ACTIVE') = 'ACTIVE'
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<String> findNotesByLogTypeAndPeriod(
        @Param("babyId") UUID babyId,
        @Param("logType") String logType,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}
