package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencyNotificationOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyNotificationOutboxRepository
        extends JpaRepository<EmergencyNotificationOutbox, UUID> {

    @Query(value = """
            SELECT pg_try_advisory_xact_lock(
                hashtextextended(CAST(:sessionId AS text), 0)
            )
            """, nativeQuery = true)
    boolean tryAcquireDeliveryLock(@Param("sessionId") UUID emergencySessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT outbox FROM EmergencyNotificationOutbox outbox "
            + "WHERE outbox.emergencySessionId = :sessionId")
    Optional<EmergencyNotificationOutbox> findForUpdate(
            @Param("sessionId") UUID emergencySessionId);

    @Query(value = """
            SELECT emergency_session_id
            FROM emergency_notification_outbox
            WHERE status = 'PENDING'
              AND next_attempt_at <= :now
            ORDER BY created_at ASC
            LIMIT 50
            """, nativeQuery = true)
    List<UUID> findDueSessionIds(@Param("now") Instant now);
}
