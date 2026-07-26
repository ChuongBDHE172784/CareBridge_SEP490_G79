package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface IEmergencySessionRepository extends JpaRepository<EmergencySession, UUID> {

    @Query(value = """
            SELECT 1 FROM pg_advisory_xact_lock(
                hashtextextended('emergency-active:' || lower(CAST(:userId AS text)), 65)
            )
            """, nativeQuery = true)
    Integer acquireUserLock(@Param("userId") UUID userId);

    @Query("SELECT e FROM EmergencySession e WHERE e.userId = :userId AND e.status = com.carebridge.backend.emergency.EmergencyStatus.ACTIVE")
    Optional<EmergencySession> findActiveByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmergencySession e WHERE e.id = :sessionId")
    Optional<EmergencySession> findByIdForUpdate(@Param("sessionId") UUID sessionId);

    @Query(value = """
            SELECT event.safety_event_id
            FROM safety_events event
            WHERE event.record_type = 'EMERGENCY_SESSION'
              AND event.status = 'ACTIVE'
              AND (
                    event.alert_status IS NULL
                 OR (event.alert_status IN ('FAILED', 'PARTIAL', 'NO_RECIPIENTS')
                     AND event.alert_updated_at <= :retryCutoff)
                 OR (event.alert_status = 'PROCESSING'
                     AND event.alert_lease_expires_at <= now())
              )
            ORDER BY event.created_at, event.safety_event_id
            LIMIT 50
            """, nativeQuery = true)
    List<UUID> findAlertRetryCandidates(@Param("retryCutoff") Instant retryCutoff);
}
