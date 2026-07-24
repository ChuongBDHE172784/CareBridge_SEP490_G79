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
        return jdbcTemplate.query("""
                INSERT INTO lifecycle_safety_outcomes (
                    outcome_id, owner_user_id, journey_id, intake_session_id,
                    emergency_session_id, risk_level, stage, origin_dashboard,
                    origin_reference_id, origin_action, occurred_at, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (intake_session_id) DO NOTHING
                RETURNING outcome_id
                """,
                resultSet -> resultSet.next() ? resultSet.getObject(1, UUID.class) : null,
                outcome.getId(), outcome.getOwnerUserId(), outcome.getJourneyId(),
                outcome.getIntakeSessionId(), outcome.getEmergencySessionId(),
                outcome.getRiskLevel().name(), outcome.getStage().name(),
                outcome.getOriginDashboard().name(), outcome.getOriginReferenceId(),
                outcome.getOriginAction().name(), Timestamp.from(outcome.getOccurredAt()),
                Timestamp.from(outcome.getRecordedAt()));
    }
}
