package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.entity.EmergencyAlertAttempt;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyAlertAttemptRepository extends JpaRepository<EmergencyAlertAttempt, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO safety_event_actions (
                safety_event_action_id, safety_event_id, action_type, idempotency_key,
                attempt_status, started_at,
                lease_expires_at, attempt_number,
                successful_recipient_count, failed_recipient_count, created_at, updated_at
            ) VALUES (:sessionId, :sessionId, 'ALERT_ATTEMPT', 'attempt:' || CAST(:sessionId AS text),
                'PROCESSING', now(), :leaseUntil, 1, 0, 0, now(), now())
            ON CONFLICT (safety_event_action_id) DO UPDATE SET
                attempt_status = 'PROCESSING',
                started_at = now(),
                completed_at = NULL,
                lease_expires_at = EXCLUDED.lease_expires_at,
                attempt_number = safety_event_actions.attempt_number + 1,
                updated_at = now()
            WHERE safety_event_actions.action_type = 'ALERT_ATTEMPT'
              AND (safety_event_actions.attempt_status IN ('FAILED', 'PARTIAL', 'NO_RECIPIENTS')
               OR (safety_event_actions.attempt_status = 'PROCESSING'
                   AND safety_event_actions.lease_expires_at <= now()))
            """, nativeQuery = true)
    int claim(@Param("sessionId") UUID sessionId, @Param("leaseUntil") Instant leaseUntil);
}
