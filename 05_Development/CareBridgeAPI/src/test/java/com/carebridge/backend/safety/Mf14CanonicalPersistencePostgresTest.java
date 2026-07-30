package com.carebridge.backend.safety;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.emergency.repository.EmergencyAlertAttemptRepository;
import com.carebridge.backend.emergency.repository.EmergencyAlertDeliveryRepository;
import com.carebridge.backend.safety.entity.SafetyEventResponseRecord;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import com.carebridge.backend.safety.repository.SafetyEventResponseRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
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
    @Autowired private SafetyEventResponseRepository responseRepository;
    @Autowired private EmergencyAlertAttemptRepository alertAttemptRepository;
    @Autowired private EmergencyAlertDeliveryRepository alertDeliveryRepository;
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

    @Test
    void persistsJpaResponseAsCanonicalSafetyAction() {
        UUID userId = insertUser();
        SafetyEvent source = insertImuEvent(userId, null, SafetyEventStatus.OPEN);

        SafetyEventResponseRecord response = responseRepository.insert(
                SafetyEventResponseRecord.builder()
                        .safetyEventId(source.getId())
                        .ownerUserId(userId)
                        .responseType("I_AM_OK")
                        .respondedAt(Instant.now())
                        .createdBy(userId)
                        .actorType("OWNER")
                        .build());

        List<Object> canonical = jdbcTemplate.queryForObject("""
                SELECT record_type,
                       alert_generation,
                       alert_successful_recipient_count,
                       alert_failed_recipient_count
                  FROM safety_events
                 WHERE safety_event_id = ?
                """, (resultSet, rowNumber) -> List.of(
                        resultSet.getString(1),
                        resultSet.getLong(2),
                        resultSet.getInt(3),
                        resultSet.getInt(4)),
                response.getId());

        assertThat(canonical).containsExactly("SAFETY_ACTION", 0L, 0, 0);
    }

    @Test
    void persistsTwoPhaseJournalAndTransitionsLinkedImuEventAtomically() {
        UUID userId = insertUser();
        UUID emergencySessionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO safety_events (
                    safety_event_id, user_id, event_type, status, record_type
                ) VALUES (?, ?, 'FALL_DETECTION', 'ACTIVE', 'EMERGENCY_SESSION')
                """, emergencySessionId, userId);
        SafetyEvent source = insertImuEvent(
                userId, emergencySessionId, SafetyEventStatus.ESCALATION_REQUESTED);

        var claim = alertAttemptRepository.claim(
                emergencySessionId, Instant.now().plus(Duration.ofMinutes(2)))
                .orElseThrow();

        UUID deviceTokenId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO device_tokens (
                    id, user_id, token, platform, active
                ) VALUES (?, ?, 'mf14-device-token', 'ANDROID', true)
                """, deviceTokenId, userId);
        var intent = alertDeliveryRepository.insertIntent(
                claim, userId, deviceTokenId, null);
        var replayedIntent = alertDeliveryRepository.insertIntent(
                claim, userId, deviceTokenId, null);
        assertThat(replayedIntent.actionId()).isEqualTo(intent.actionId());
        assertThat(alertDeliveryRepository.appendResult(
                intent.actionId(), claim, true, 1, "mf14-message", null)).isTrue();
        assertThat(alertDeliveryRepository.appendResult(
                intent.actionId(), claim, true, 1, "mf14-message", null)).isFalse();

        assertThat(alertAttemptRepository.complete(
                claim, "SENT", 1, 0, false)).isTrue();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT status FROM safety_events WHERE safety_event_id = ?
                """, String.class, source.getId()))
                .isEqualTo("EMERGENCY_ALERT_SENT");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE parent_event_id = ? AND action_type = 'ALERT_ATTEMPT'
                """, Long.class, emergencySessionId))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE parent_event_id = ? AND action_type = 'DELIVERY'
                """, Long.class, emergencySessionId))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE parent_event_id = ? AND action_type = 'FAMILY_ALERT'
                """, Long.class, emergencySessionId))
                .isEqualTo(1L);
    }

    @Test
    void synchronizesImuEventWhenLinkedEmergencySessionWasAlreadySent() {
        UUID userId = insertUser();
        UUID emergencySessionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO safety_events (
                    safety_event_id, user_id, event_type, status, record_type,
                    alert_status
                ) VALUES (?, ?, 'FALL_DETECTION', 'ACTIVE',
                          'EMERGENCY_SESSION', 'SENT')
                """, emergencySessionId, userId);
        SafetyEvent source = insertImuEvent(
                userId, emergencySessionId, SafetyEventStatus.ESCALATION_REQUESTED);

        assertThat(safetyEventRepository.transitionLinkedEventForSentEmergencySession(
                source.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT status FROM safety_events WHERE safety_event_id = ?
                """, String.class, source.getId()))
                .isEqualTo("EMERGENCY_ALERT_SENT");
        assertThat(safetyEventRepository.transitionLinkedEventForSentEmergencySession(
                source.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE parent_event_id = ? AND action_type = 'FAMILY_ALERT'
                """, Long.class, emergencySessionId))
                .isZero();

        UUID resolvedSessionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO safety_events (
                    safety_event_id, user_id, event_type, status, record_type,
                    alert_status
                ) VALUES (?, ?, 'FALL_DETECTION', 'RESOLVED',
                          'EMERGENCY_SESSION', 'SENT')
                """, resolvedSessionId, userId);
        SafetyEvent resolvedSource = insertImuEvent(
                userId, resolvedSessionId, SafetyEventStatus.ESCALATION_REQUESTED);
        assertThat(safetyEventRepository.transitionLinkedEventForSentEmergencySession(
                resolvedSource.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT))
                .isZero();
    }

    @Test
    void acceptsCanonicalTriageEscalationAction() {
        UUID userId = insertUser();
        UUID emergencySessionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO safety_events (
                    safety_event_id, user_id, event_type, status, record_type
                ) VALUES (?, ?, 'AUTO_TRIAGE', 'ACTIVE', 'EMERGENCY_SESSION')
                """, emergencySessionId, userId);

        assertThat(jdbcTemplate.update("""
                INSERT INTO safety_events (
                    parent_event_id, record_type, event_type, action_type,
                    user_id, action_phase, idempotency_key
                ) VALUES (?, 'SAFETY_ACTION', 'ACTION', 'TRIAGE_ESCALATION',
                          ?, 'LINKED', ?)
                """, emergencySessionId, userId, "mf14-triage:" + emergencySessionId))
                .isEqualTo(1);
    }

    private SafetyEvent insertImuEvent(
            UUID userId,
            UUID emergencySessionId,
            SafetyEventStatus status) {
        UUID monitoringSessionId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO safety_monitoring_sessions (monitoring_session_id, user_id)
                VALUES (?, ?)
                """, monitoringSessionId, userId);
        return safetyEventRepository.saveAndFlush(SafetyEvent.builder()
                .userId(userId)
                .imuSessionId(monitoringSessionId)
                .eventType(SafetyEventType.SUSPECTED_FALL)
                .magnitude(BigDecimal.valueOf(30))
                .detectedAt(Instant.now())
                .status(status)
                .emergencySessionId(emergencySessionId)
                .signalKey("mf14-" + UUID.randomUUID())
                .createdBy("SYSTEM")
                .build());
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
