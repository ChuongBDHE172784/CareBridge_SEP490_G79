package com.carebridge.backend.emergency.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Canonical many-to-one association between a completed RED triage intake and
 * the emergency session opened or reused for it.
 */
@Repository
@RequiredArgsConstructor
public class TriageEmergencyEscalationLinkRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<UUID> findEmergencySessionId(UUID intakeSessionId, UUID ownerUserId) {
        return jdbcTemplate.query("""
                SELECT action.safety_event_id
                  FROM safety_event_actions action
                 WHERE action.action_type = 'TRIAGE_ESCALATION'
                   AND action.triage_handoff_id = ?
                   AND action.owner_user_id = ?
                 ORDER BY action.created_at, action.safety_event_action_id
                 LIMIT 1
                """, resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getObject(1, UUID.class))
                        : Optional.empty(), intakeSessionId, ownerUserId);
    }

    public UUID linkIfAbsent(
            UUID intakeSessionId,
            UUID emergencySessionId,
            UUID ownerUserId,
            Instant linkedAt) {
        jdbcTemplate.update("""
                INSERT INTO safety_event_actions (
                    safety_event_action_id, safety_event_id, action_type,
                    owner_user_id, triage_handoff_id, attempt_number,
                    idempotency_key, action_phase, alert_generation, created_at
                ) VALUES (
                    gen_random_uuid(), ?, 'TRIAGE_ESCALATION', ?, ?, 1,
                    'triage-escalation:' || CAST(? AS text), 'LINKED', 0, ?
                )
                ON CONFLICT (idempotency_key) DO NOTHING
                """, emergencySessionId, ownerUserId, intakeSessionId,
                intakeSessionId, Timestamp.from(linkedAt));

        return findEmergencySessionId(intakeSessionId, ownerUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "Canonical triage emergency escalation link was not persisted"));
    }
}
