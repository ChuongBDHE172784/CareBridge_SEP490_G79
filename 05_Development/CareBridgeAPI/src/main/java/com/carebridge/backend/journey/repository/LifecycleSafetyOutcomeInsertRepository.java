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
                INSERT INTO mother_journey_events (
                    event_id, mother_journey_id, owner_user_id, event_type,
                    event_payload_jsonb, schema_version, actor_user_id,
                    effective_at, recorded_at, legacy_source, legacy_id,
                    triage_session_id, emergency_session_id, risk_level, stage,
                    origin_dashboard, origin_reference_id, origin_action)
                VALUES (?, ?, ?, 'SAFETY_OUTCOME',
                    jsonb_build_object(
                        'intakeSessionId', CAST(? AS text),
                        'emergencySessionId', CAST(? AS text),
                        'riskLevel', ?,
                        'stage', ?,
                        'originDashboard', ?,
                        'originReferenceId', CAST(? AS text),
                        'originAction', ?),
                    '1', ?, ?, ?, 'SAFETY_OUTCOME', CAST(? AS text),
                    ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (legacy_source, legacy_id) DO NOTHING
                """,
                outcome.getId(), outcome.getJourneyId(), outcome.getOwnerUserId(),
                outcome.getIntakeSessionId(), outcome.getEmergencySessionId(),
                outcome.getRiskLevel().name(), outcome.getStage().name(),
                outcome.getOriginDashboard().name(), outcome.getOriginReferenceId(),
                outcome.getOriginAction().name(), outcome.getOwnerUserId(),
                Timestamp.from(outcome.getOccurredAt()), Timestamp.from(outcome.getRecordedAt()),
                outcome.getIntakeSessionId(),
                outcome.getIntakeSessionId(), outcome.getEmergencySessionId(),
                outcome.getRiskLevel().name(), outcome.getStage().name(),
                outcome.getOriginDashboard().name(), outcome.getOriginReferenceId(),
                outcome.getOriginAction().name());
        return inserted == 1 ? outcome.getId() : null;
    }
}
