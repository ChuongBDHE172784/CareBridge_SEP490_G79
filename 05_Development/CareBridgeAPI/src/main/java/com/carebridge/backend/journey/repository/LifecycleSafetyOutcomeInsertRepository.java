package com.carebridge.backend.journey.repository;

import com.carebridge.backend.journey.entity.LifecycleSafetyOutcome;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LifecycleSafetyOutcomeInsertRepository {
    private final JdbcTemplate jdbcTemplate;

    public UUID insertIfAbsent(LifecycleSafetyOutcome outcome) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO audit_events (
                    audit_event_id, event_category, actor_user_id, subject_user_id,
                    subject_reference_id, resource_type, resource_id,
                    payload, occurred_at, created_at)
                VALUES (?, 'SAFETY_OUTCOME', ?, ?, ?, 'mother_journeys', ?,
                    jsonb_build_object(
                        'intakeSessionId', CAST(? AS text),
                        'triageSessionId', CAST(? AS text),
                        'emergencySessionId', CAST(? AS text),
                        'riskLevel', ?,
                        'stage', ?,
                        'originDashboard', ?,
                        'originReferenceId', CAST(? AS text),
                        'originAction', ?),
                    ?, ?)
                ON CONFLICT (audit_event_id) DO NOTHING
                """,
                outcome.getId(), outcome.getOwnerUserId(), outcome.getOwnerUserId(),
                outcome.getJourneyId(), outcome.getJourneyId(),
                outcome.getIntakeSessionId(), outcome.getIntakeSessionId(),
                outcome.getEmergencySessionId(),
                outcome.getRiskLevel().name(), outcome.getStage().name(),
                outcome.getOriginDashboard().name(), outcome.getOriginReferenceId(),
                outcome.getOriginAction().name(),
                Timestamp.from(outcome.getOccurredAt()), Timestamp.from(outcome.getRecordedAt()));
        return inserted == 1 ? outcome.getId() : null;
    }
}
