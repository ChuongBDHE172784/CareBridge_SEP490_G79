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
    @Query("""
            update SafetyEvent event
               set event.status = :targetStatus
             where event.emergencySessionId = :emergencySessionId
               and event.status = :expectedStatus
               and event.recordType = 'IMU_EVENT'
            """)
    int transitionAlertSentByEmergencySessionId(
            @Param("emergencySessionId") UUID emergencySessionId,
            @Param("expectedStatus") SafetyEventStatus expectedStatus,
            @Param("targetStatus") SafetyEventStatus targetStatus);

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    Integer acquireSignalLock(@Param("lockKey") String lockKey);
}
