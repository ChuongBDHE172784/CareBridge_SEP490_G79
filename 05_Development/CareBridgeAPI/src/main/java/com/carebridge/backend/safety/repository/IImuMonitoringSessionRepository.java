package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface IImuMonitoringSessionRepository extends JpaRepository<ImuMonitoringSession, UUID> {

    @Query(value = """
            SELECT 1 FROM pg_advisory_xact_lock(
                hashtextextended(
                    'safety-monitoring-active:' || lower(CAST(:userId AS text)),
                    65
                )
            )
            """, nativeQuery = true)
    Integer acquireUserLock(@Param("userId") UUID userId);

    @Query("SELECT s FROM ImuMonitoringSession s WHERE s.userId = :userId AND s.status = com.carebridge.backend.safety.ImuSessionStatus.ACTIVE")
    Optional<ImuMonitoringSession> findActiveByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ImuMonitoringSession s WHERE s.userId = :userId AND s.status = com.carebridge.backend.safety.ImuSessionStatus.ACTIVE")
    Optional<ImuMonitoringSession> findActiveForUpdateByUserId(@Param("userId") UUID userId);
}
