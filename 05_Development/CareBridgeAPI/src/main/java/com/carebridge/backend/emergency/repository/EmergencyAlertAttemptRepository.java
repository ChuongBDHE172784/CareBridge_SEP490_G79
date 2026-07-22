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
            INSERT INTO emergency_alert_attempts (
                emergency_session_id, status, started_at,
                lease_expires_at, attempt_number,
                successful_recipient_count, failed_recipient_count, updated_at
            ) VALUES (:sessionId, 'PROCESSING', now(), :leaseUntil, 1, 0, 0, now())
            ON CONFLICT (emergency_session_id) DO UPDATE SET
                status = 'PROCESSING',
                started_at = now(),
                completed_at = NULL,
                lease_expires_at = EXCLUDED.lease_expires_at,
                attempt_number = emergency_alert_attempts.attempt_number + 1,
                updated_at = now()
            WHERE emergency_alert_attempts.status IN ('FAILED', 'PARTIAL', 'NO_RECIPIENTS')
               OR (emergency_alert_attempts.status = 'PROCESSING'
                   AND emergency_alert_attempts.lease_expires_at <= now())
            """, nativeQuery = true)
    int claim(@Param("sessionId") UUID sessionId, @Param("leaseUntil") Instant leaseUntil);
}
