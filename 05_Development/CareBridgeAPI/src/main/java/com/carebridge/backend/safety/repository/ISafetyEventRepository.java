package com.carebridge.backend.safety.repository;

import com.carebridge.backend.safety.entity.SafetyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import java.util.UUID;
import java.util.Optional;
import java.time.Instant;
import java.util.List;
import com.carebridge.backend.safety.SafetyEventStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ISafetyEventRepository extends JpaRepository<SafetyEvent, UUID> {
    Page<SafetyEvent> findByUserIdOrderByDetectedAtDesc(UUID userId, Pageable pageable);
    Optional<SafetyEvent> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from SafetyEvent event where event.id = :id and event.userId = :userId")
    Optional<SafetyEvent> findLockedByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    Optional<SafetyEvent> findByImuSessionIdAndSignalKey(UUID imuSessionId, String signalKey);

    List<SafetyEvent> findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(
            SafetyEventStatus status, Instant deadline);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE safety_events event
               SET status = :#{#targetStatus.name()},
                   updated_at = now()
             WHERE event.emergency_session_id = :emergencySessionId
               AND event.status = :#{#expectedStatus.name()}
               AND event.record_type = 'IMU_EVENT'
            """, nativeQuery = true)
    int transitionAlertSentByEmergencySessionId(
            @Param("emergencySessionId") UUID emergencySessionId,
            @Param("expectedStatus") SafetyEventStatus expectedStatus,
            @Param("targetStatus") SafetyEventStatus targetStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE safety_events event
               SET status = :#{#targetStatus.name()},
                   updated_at = now()
              FROM safety_events session
             WHERE event.safety_event_id = :safetyEventId
               AND event.status = :#{#expectedStatus.name()}
               AND event.record_type = 'IMU_EVENT'
               AND event.emergency_session_id = session.safety_event_id
               AND session.record_type = 'EMERGENCY_SESSION'
               AND session.status = 'ACTIVE'
               AND session.alert_status = 'SENT'
            """, nativeQuery = true)
    int transitionLinkedEventForSentEmergencySession(
            @Param("safetyEventId") UUID safetyEventId,
            @Param("expectedStatus") SafetyEventStatus expectedStatus,
            @Param("targetStatus") SafetyEventStatus targetStatus);

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    Integer acquireSignalLock(@Param("lockKey") String lockKey);
}
