package com.carebridge.backend.emergency.repository;

import com.carebridge.backend.emergency.service.EmergencyAlertClaim;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Atomic parent-state claims plus immutable ALERT_ATTEMPT/FAMILY_ALERT snapshots. */
@Repository
@RequiredArgsConstructor
public class EmergencyAlertAttemptRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<EmergencyAlertClaim> claim(UUID sessionId, Instant leaseUntil) {
        return jdbcTemplate.query("""
                WITH claimed AS (
                    UPDATE safety_events event
                       SET alert_generation = event.alert_generation + 1,
                           alert_status = 'PROCESSING',
                           alert_claim_token = gen_random_uuid(),
                           alert_claimed_at = now(),
                           alert_lease_expires_at = ?,
                           alert_completed_at = NULL,
                           alert_successful_recipient_count = 0,
                           alert_failed_recipient_count = 0,
                           alert_updated_at = now(),
                           updated_at = now()
                     WHERE event.safety_event_id = ?
                       AND event.record_type = 'EMERGENCY_SESSION'
                       AND event.status = 'ACTIVE'
                       AND (
                            event.alert_status IS NULL
                            OR event.alert_status IN ('FAILED','PARTIAL','NO_RECIPIENTS')
                            OR (event.alert_status = 'PROCESSING'
                                AND event.alert_lease_expires_at <= now())
                       )
                    RETURNING event.safety_event_id, event.user_id,
                              event.alert_generation, event.alert_claim_token,
                              event.alert_lease_expires_at
                ), snapshot AS (
                    INSERT INTO safety_event_actions (
                        safety_event_action_id, safety_event_id, action_type,
                        owner_user_id, attempt_number, idempotency_key,
                        action_phase, alert_generation, fence_token,
                        attempt_status, started_at, lease_expires_at,
                        successful_recipient_count, failed_recipient_count,
                        created_at, updated_at
                    )
                    SELECT gen_random_uuid(), claimed.safety_event_id, 'ALERT_ATTEMPT',
                           claimed.user_id,
                           least(claimed.alert_generation, 2147483647)::integer,
                           'alert-attempt:' || claimed.safety_event_id::text || ':'
                               || claimed.alert_generation::text || ':started',
                           'STARTED', claimed.alert_generation, claimed.alert_claim_token,
                           'PROCESSING', now(), claimed.alert_lease_expires_at,
                           0, 0, now(), now()
                      FROM claimed
                    RETURNING safety_event_id
                )
                SELECT claimed.safety_event_id, claimed.alert_generation,
                       claimed.alert_claim_token, claimed.alert_lease_expires_at
                  FROM claimed
                  JOIN snapshot USING (safety_event_id)
                """, resultSet -> {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new EmergencyAlertClaim(
                            resultSet.getObject("safety_event_id", UUID.class),
                            resultSet.getLong("alert_generation"),
                            resultSet.getObject("alert_claim_token", UUID.class),
                            resultSet.getTimestamp("alert_lease_expires_at").toInstant()));
                }, Timestamp.from(leaseUntil), sessionId);
    }

    public boolean renew(EmergencyAlertClaim claim, Instant leaseUntil) {
        return jdbcTemplate.update("""
                UPDATE safety_events event
                   SET alert_lease_expires_at = ?,
                       alert_updated_at = now(),
                       updated_at = now()
                 WHERE event.safety_event_id = ?
                   AND event.record_type = 'EMERGENCY_SESSION'
                   AND event.status = 'ACTIVE'
                   AND event.alert_status = 'PROCESSING'
                   AND event.alert_generation = ?
                   AND event.alert_claim_token = ?
                   AND event.alert_lease_expires_at > now()
                """, Timestamp.from(leaseUntil), claim.emergencySessionId(),
                claim.generation(), claim.fenceToken()) == 1;
    }

    public boolean complete(
            EmergencyAlertClaim claim,
            String status,
            int successfulRecipients,
            int failedRecipients,
            boolean locationIncluded) {
        Integer completed = jdbcTemplate.queryForObject("""
                WITH finished AS (
                    UPDATE safety_events event
                       SET alert_status = ?,
                           alert_completed_at = now(),
                           alert_successful_recipient_count = ?,
                           alert_failed_recipient_count = ?,
                           alert_lease_expires_at = NULL,
                           alert_updated_at = now(),
                           updated_at = now()
                     WHERE event.safety_event_id = ?
                       AND event.record_type = 'EMERGENCY_SESSION'
                       AND event.status = 'ACTIVE'
                       AND event.alert_status = 'PROCESSING'
                       AND event.alert_generation = ?
                       AND event.alert_claim_token = ?
                       AND event.alert_lease_expires_at > now()
                    RETURNING event.safety_event_id, event.user_id,
                              event.alert_generation, event.alert_claim_token,
                              event.alert_claimed_at
                ), attempt_result AS (
                    INSERT INTO safety_event_actions (
                        safety_event_action_id, safety_event_id, action_type,
                        owner_user_id, attempt_number, idempotency_key,
                        action_phase, alert_generation, fence_token,
                        attempt_status, started_at, completed_at,
                        successful_recipient_count, failed_recipient_count,
                        created_at, updated_at
                    )
                    SELECT gen_random_uuid(), finished.safety_event_id, 'ALERT_ATTEMPT',
                           finished.user_id,
                           least(finished.alert_generation, 2147483647)::integer,
                           'alert-attempt:' || finished.safety_event_id::text || ':'
                               || finished.alert_generation::text || ':result',
                           'RESULT', finished.alert_generation, finished.alert_claim_token,
                           ?, finished.alert_claimed_at, now(), ?, ?, now(), now()
                      FROM finished
                    RETURNING safety_event_id
                ), family_result AS (
                    INSERT INTO safety_event_actions (
                        safety_event_action_id, safety_event_id, action_type,
                        owner_user_id, attempt_number, idempotency_key,
                        action_phase, alert_generation, fence_token,
                        recipient_count, location_included, created_by_text,
                        created_at, delivered_at
                    )
                    SELECT gen_random_uuid(), finished.safety_event_id, 'FAMILY_ALERT',
                           finished.user_id,
                           least(finished.alert_generation, 2147483647)::integer,
                           'family-alert:' || finished.safety_event_id::text || ':'
                               || finished.alert_generation::text,
                           'RESULT', finished.alert_generation, finished.alert_claim_token,
                           ?, ?, 'SYSTEM', now(), now()
                      FROM finished
                     WHERE ? > 0
                    RETURNING safety_event_id
                )
                SELECT count(*)::integer FROM attempt_result
                """, Integer.class,
                status, successfulRecipients, failedRecipients,
                claim.emergencySessionId(), claim.generation(), claim.fenceToken(),
                status, successfulRecipients, failedRecipients,
                successfulRecipients, locationIncluded, successfulRecipients);
        return completed != null && completed == 1;
    }
}
