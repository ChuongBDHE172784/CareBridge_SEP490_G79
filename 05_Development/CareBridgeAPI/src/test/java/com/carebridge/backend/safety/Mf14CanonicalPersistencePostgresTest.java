package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@EnabledIfSystemProperty(named = "gate0.enabled", matches = "true")
class Mf14CanonicalPersistencePostgresTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ISafetyEventRepository safetyEventRepository;
    @Autowired private CareGroupMemberRepository careGroupMemberRepository;

    @Test
    void persistsCanonicalImuEventWithZeroAlertCounters() {
        UUID userId = insertUser();
        UUID sessionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO safety_monitoring_sessions (monitoring_session_id, user_id)
                VALUES (?, ?)
                """, sessionId, userId);

        SafetyEvent saved = safetyEventRepository.saveAndFlush(SafetyEvent.builder()
                .userId(userId)
                .imuSessionId(sessionId)
                .eventType(SafetyEventType.SUSPECTED_FALL)
                .magnitude(BigDecimal.valueOf(30))
                .detectedAt(Instant.parse("2026-07-28T00:00:00Z"))
                .signalKey("mf14-postgres-signal")
                .createdBy("SYSTEM")
                .build());

        List<Integer> counters = jdbcTemplate.queryForObject("""
                SELECT alert_generation::integer,
                       alert_successful_recipient_count,
                       alert_failed_recipient_count
                  FROM safety_events
                 WHERE safety_event_id = ?
                """, (resultSet, rowNumber) -> List.of(
                        resultSet.getInt(1),
                        resultSet.getInt(2),
                        resultSet.getInt(3)),
                saved.getId());

        assertThat(counters).containsExactly(0, 0, 0);
    }

    @Test
    void executesEmergencyContactQueryAndPreservesPriority() {
        UUID ownerId = insertUser();
        UUID priorityOne = insertUser();
        UUID priorityTwo = insertUser();
        UUID notDesignated = insertUser();
        UUID careGroupId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_groups (
                    care_group_id, owner_user_id, group_name, status
                ) VALUES (?, ?, 'MF-14 family', 'ACTIVE')
                """, careGroupId, ownerId);
        insertMember(careGroupId, priorityTwo, true, 2, "ACCEPTED");
        insertMember(careGroupId, priorityOne, true, 1, "ACCEPTED");
        insertMember(careGroupId, notDesignated, false, 0, "ACCEPTED");

        assertThat(careGroupMemberRepository.findEmergencyContactUserIds(ownerId))
                .containsExactly(priorityOne, priorityTwo);
    }

    private UUID insertUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (user_id, created_at, updated_at, person_id)
                VALUES (?, now(), now(), ?)
                """, userId, UUID.randomUUID());
        return userId;
    }

    private void insertMember(
            UUID careGroupId,
            UUID userId,
            boolean emergencyContact,
            int priority,
            String invitationStatus) {
        jdbcTemplate.update("""
                INSERT INTO care_group_members (
                    care_group_id,
                    user_id,
                    invitation_status,
                    is_emergency_contact,
                    emergency_contact_priority
                ) VALUES (?, ?, ?, ?, ?)
                """, careGroupId, userId, invitationStatus, emergencyContact, priority);
    }
}
