package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Execution(ExecutionMode.SAME_THREAD)
class EmergencyTriageLinkPostgresIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private IEmergencyService emergencyService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private IFamilyAlertService familyAlertService;

    private UUID ownerId;
    private UUID firstIntakeId;
    private UUID secondIntakeId;

    @BeforeEach
    void seedCompletedRedIntakes() {
        ownerId = UUID.randomUUID();
        firstIntakeId = UUID.randomUUID();
        secondIntakeId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO persons(person_id,display_name,created_at,updated_at)
                VALUES (?, 'RED Link Owner', now(), now())
                """, ownerId);
        jdbcTemplate.update("""
                INSERT INTO users(
                    user_id,person_id,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES (?, ?, ?, 'MOTHER', 'ACTIVE', true, false, true, false,
                        now(), now())
                """, ownerId, ownerId, "red-link-" + ownerId + "@test");
        seedRedIntake(firstIntakeId, "first red intake");
        seedRedIntake(secondIntakeId, "second red intake");
    }

    @Test
    void firstRedFlushesParentSecondReusesAndReplayReturnsCanonicalEmergency() {
        var first = emergencyService.openOrReuseFromTriage(firstIntakeId, ownerId);
        var second = emergencyService.openOrReuseFromTriage(secondIntakeId, ownerId);
        var replay = emergencyService.openOrReuseFromTriage(firstIntakeId, ownerId);

        assertThat(second.getSessionId()).isEqualTo(first.getSessionId());
        assertThat(replay.getSessionId()).isEqualTo(first.getSessionId());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE user_id = ? AND record_type = 'EMERGENCY_SESSION'
                   AND status = 'ACTIVE'
                """, Long.class, ownerId)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_event_actions
                 WHERE owner_user_id = ? AND action_type = 'TRIAGE_ESCALATION'
                   AND safety_event_id = ?
                """, Long.class, ownerId, first.getSessionId())).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_event_actions
                 WHERE owner_user_id = ? AND action_type = 'TRIAGE_ESCALATION'
                   AND triage_handoff_id IN (?, ?)
                """, Long.class, ownerId, firstIntakeId, secondIntakeId))
                .isEqualTo(2L);
    }

    private void seedRedIntake(UUID intakeId, String symptoms) {
        jdbcTemplate.update("""
                INSERT INTO triage_sessions (
                    triage_session_id,user_id,stage,symptoms,risk_level,status,
                    emergency,created_at,completed_at,created_by)
                VALUES (?, ?, 'PREGNANCY', ?, 'RED', 'COMPLETED', true,
                        now(), now(), ?)
                """, intakeId, ownerId, symptoms, ownerId);
    }
}
