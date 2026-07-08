package com.carebridge.backend.carejourney.repository;

import com.carebridge.backend.carejourney.entity.BabyDailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BabyDailyLogRepository extends JpaRepository<BabyDailyLog, UUID> {

    List<BabyDailyLog> findByBabyId(UUID babyId);

    @Query(value = """
        SELECT log_type AS logType,
               COUNT(*) AS count,
               COALESCE(SUM(quantity), 0) AS totalQuantity,
               MAX(quantity) AS maxQuantity,
               MAX(unit) AS unit
        FROM baby_daily_logs
        WHERE baby_id = :babyId
          AND created_at >= :fromDate
          AND created_at <= :toDate
        GROUP BY log_type
        """, nativeQuery = true)
    List<LogTypeAggregateRow> aggregateByLogType(
        @Param("babyId") UUID babyId,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );

    @Query(value = """
        SELECT note FROM baby_daily_logs
        WHERE baby_id = :babyId
          AND log_type = :logType
          AND created_at >= :fromDate
          AND created_at <= :toDate
          AND note IS NOT NULL
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<String> findNotesByLogTypeAndPeriod(
        @Param("babyId") UUID babyId,
        @Param("logType") String logType,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}
