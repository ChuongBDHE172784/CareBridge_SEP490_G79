package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.service.AlertRecipientEndpoint;
import com.carebridge.backend.emergency.service.EmergencyAlertAttemptService;
import com.carebridge.backend.emergency.service.EmergencyAlertDeliveryPersistenceService;
import com.carebridge.backend.emergency.service.EmergencyAlertProviderFence;
import com.carebridge.backend.emergency.service.EmergencyAlertRetryJob;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.SmsFallbackPort;
import com.carebridge.backend.emergency.service.impl.FamilyAlertService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Execution(ExecutionMode.SAME_THREAD)
class EmergencyTriageLinkPostgresIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired private IEmergencyService emergencyService;
    @Autowired private IEmergencySessionRepository emergencySessionRepository;
    @Autowired private EmergencyAlertAttemptService alertAttemptService;
    @Autowired private EmergencyAlertProviderFence providerFence;
    @Autowired private EmergencyAlertDeliveryPersistenceService deliveryPersistenceService;
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
                INSERT INTO users(
                    user_id,person_id,display_name,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES (?, ?, 'RED Link Owner', ?, 'MOTHER', 'ACTIVE', true, false, true, false,
                        now(), now())
                """, ownerId, ownerId, "red-link-" + ownerId + "@test");
        seedRedIntake(firstIntakeId, "first red intake");
        seedRedIntake(secondIntakeId, "second red intake");
    }

    @Test
    void firstRedFlushesParentSecondReusesAndReplayReturnsCanonicalEmergency() {
        jdbcTemplate.execute("""
                ALTER TABLE safety_events
                    ALTER COLUMN alert_successful_recipient_count DROP DEFAULT,
                    ALTER COLUMN alert_failed_recipient_count DROP DEFAULT
                """);

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
                SELECT count(*) FROM safety_events
                 WHERE user_id = ? AND action_type = 'TRIAGE_ESCALATION'
                   AND parent_event_id = ?
                """, Long.class, ownerId, first.getSessionId())).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE user_id = ? AND action_type = 'TRIAGE_ESCALATION'
                   AND triage_handoff_id IN (?, ?)
                """, Long.class, ownerId, firstIntakeId, secondIntakeId))
                .isEqualTo(2L);
        assertEscalationCountersZero(firstIntakeId);
        assertEscalationCountersZero(secondIntakeId);
    }

    @Test
    void retryCandidateQueryHonorsStatusAgeLeaseAndActivityMatrix() {
        UUID sessionId = emergencyService.openOrReuseFromTriage(firstIntakeId, ownerId)
                .getSessionId();
        Instant cutoff = Instant.now().minusSeconds(60);

        assertThat(emergencySessionRepository.findAlertRetryCandidates(cutoff))
                .contains(sessionId);

        assertRetryStatusHonorsCutoff(sessionId, "FAILED", cutoff);
        assertRetryStatusHonorsCutoff(sessionId, "PARTIAL", cutoff);
        assertRetryStatusHonorsCutoff(sessionId, "NO_RECIPIENTS", cutoff);

        jdbcTemplate.update("""
                UPDATE safety_events
                   SET alert_status='PROCESSING', alert_generation=alert_generation+1,
                       alert_claim_token=?, alert_claimed_at=now() - interval '2 minutes',
                       alert_lease_expires_at=now() - interval '1 second',
                       alert_updated_at=now()
                 WHERE safety_event_id=?
                """, UUID.randomUUID(), sessionId);
        assertThat(emergencySessionRepository.findAlertRetryCandidates(cutoff))
                .contains(sessionId);

        jdbcTemplate.update("""
                UPDATE safety_events
                   SET alert_lease_expires_at=now() + interval '2 minutes'
                 WHERE safety_event_id=?
                """, sessionId);
        assertThat(emergencySessionRepository.findAlertRetryCandidates(cutoff))
                .doesNotContain(sessionId);

        jdbcTemplate.update("""
                UPDATE safety_events
                   SET status='RESOLVED', resolved_at=now(),
                       alert_status='FAILED', alert_updated_at=now() - interval '2 minutes',
                       alert_lease_expires_at=NULL
                 WHERE safety_event_id=?
                """, sessionId);
        assertThat(emergencySessionRepository.findAlertRetryCandidates(cutoff))
                .doesNotContain(sessionId);
    }

    @Test
    void retryCandidateQueryUsesStableCreatedAtOrderingAndLimitFifty() {
        Instant oldest = Instant.parse("1900-01-01T00:00:00Z");
        List<UUID> expected = new ArrayList<>();
        for (int index = 0; index < 52; index++) {
            UUID userId = UUID.fromString(String.format(
                    "20000000-0000-0000-0000-%012d", index + 1));
            UUID eventId = UUID.fromString(String.format(
                    "10000000-0000-0000-0000-%012d", index + 1));
            seedRetryCandidate(userId, eventId, oldest.plusSeconds(index / 2));
            expected.add(eventId);
        }

        assertThat(emergencySessionRepository.findAlertRetryCandidates(
                Instant.now().minusSeconds(60)))
                .containsExactlyElementsOf(expected.subList(0, 50));
    }

    @Test
    void ov01E2e014RestartReclaimsExpiredAttemptWithoutResendingSuccessfulDevice() {
        UUID sessionId = emergencyService.openOrReuseFromTriage(firstIntakeId, ownerId)
                .getSessionId();
        UUID recipientOne = UUID.randomUUID();
        UUID recipientTwo = UUID.randomUUID();
        UUID deviceOne = UUID.randomUUID();
        UUID deviceTwo = UUID.randomUUID();
        seedRecipientDevice(recipientOne, deviceOne, "restart-token-1");
        seedRecipientDevice(recipientTwo, deviceTwo, "restart-token-2");
        jdbcTemplate.update("""
                UPDATE safety_events SET created_at='1800-01-01T00:00:00Z'
                 WHERE safety_event_id=?
                """, sessionId);

        FamilyMemberPort familyMembers = mock(FamilyMemberPort.class);
        FcmNotificationPort fcm = mock(FcmNotificationPort.class);
        LocationConsentPort locationConsent = mock(LocationConsentPort.class);
        SmsFallbackPort sms = mock(SmsFallbackPort.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        AuditService audit = mock(AuditService.class);
        when(familyMembers.getFamilyAlertRecipients(ownerId)).thenReturn(List.of(
                new AlertRecipientEndpoint(recipientOne, deviceOne, "restart-token-1"),
                new AlertRecipientEndpoint(recipientTwo, deviceTwo, "restart-token-2")));
        when(fcm.send(eq("restart-token-1"), any()))
                .thenReturn(FcmDeliveryResult.success("fcm-one", 1));
        when(fcm.send(eq("restart-token-2"), any()))
                .thenReturn(FcmDeliveryResult.failed("TRANSIENT", 1),
                        FcmDeliveryResult.success("fcm-two", 1));

        FamilyAlertService realDeliveryService = new FamilyAlertService(
                familyMembers, fcm, locationConsent, sms, events, alertAttemptService,
                providerFence, deliveryPersistenceService, audit);
        AtomicReference<RuntimeException> dispatchFailure = new AtomicReference<>();
        IFamilyAlertService recordingDeliveryService = event -> {
            try {
                realDeliveryService.sendAlert(event);
            } catch (RuntimeException exception) {
                dispatchFailure.set(exception);
                throw exception;
            }
        };
        new EmergencyAlertRetryJob(emergencySessionRepository, recordingDeliveryService)
                .retryPendingAlerts();
        assertThat(dispatchFailure.get()).isNull();
        assertThat(alertStatus(sessionId)).isEqualTo("PARTIAL");

        var interruptedClaim = alertAttemptService.claim(sessionId).orElseThrow();
        jdbcTemplate.update("""
                UPDATE safety_events
                   SET alert_lease_expires_at=now() - interval '1 second',
                       alert_updated_at=now()
                 WHERE safety_event_id=? AND alert_claim_token=?
                """, sessionId, interruptedClaim.fenceToken());

        dispatchFailure.set(null);
        FamilyAlertService restartedDeliveryService = new FamilyAlertService(
                familyMembers, fcm, locationConsent, sms, events, alertAttemptService,
                providerFence, deliveryPersistenceService, audit);
        IFamilyAlertService restartedRecordingService = event -> {
            try {
                restartedDeliveryService.sendAlert(event);
            } catch (RuntimeException exception) {
                dispatchFailure.set(exception);
                throw exception;
            }
        };
        new EmergencyAlertRetryJob(emergencySessionRepository, restartedRecordingService)
                .retryPendingAlerts();

        assertThat(dispatchFailure.get()).isNull();
        verify(fcm, times(1)).send(eq("restart-token-1"), any());
        verify(fcm, times(2)).send(eq("restart-token-2"), any());
        assertThat(alertStatus(sessionId)).isEqualTo("SENT");
        assertThat(deliveryResultCount(sessionId, deviceOne, "SENT")).isOne();
        assertThat(deliveryResultCount(sessionId, deviceTwo, "FAILED")).isOne();
        assertThat(deliveryResultCount(sessionId, deviceTwo, "SENT")).isOne();
    }

    private void assertRetryStatusHonorsCutoff(UUID sessionId, String status, Instant cutoff) {
        jdbcTemplate.update("""
                UPDATE safety_events
                   SET alert_status=?, alert_updated_at=now()
                 WHERE safety_event_id=?
                """, status, sessionId);
        assertThat(emergencySessionRepository.findAlertRetryCandidates(cutoff))
                .doesNotContain(sessionId);
        jdbcTemplate.update("""
                UPDATE safety_events
                   SET alert_updated_at=now() - interval '2 minutes'
                 WHERE safety_event_id=?
                """, sessionId);
        assertThat(emergencySessionRepository.findAlertRetryCandidates(cutoff))
                .contains(sessionId);
    }

    private void seedRetryCandidate(UUID userId, UUID eventId, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO users(
                    user_id,person_id,display_name,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES (?, ?, 'Retry Matrix Owner', ?, 'MOTHER', 'ACTIVE', true, false,
                        true, false, ?, ?)
                """, userId, userId, "retry-matrix-" + userId + "@test",
                Timestamp.from(createdAt), Timestamp.from(createdAt));
        jdbcTemplate.update("""
                INSERT INTO safety_events(
                    safety_event_id,user_id,detected_at,event_type,status,record_type,
                    created_at,updated_at)
                VALUES (?, ?, ?, 'MANUAL', 'ACTIVE', 'EMERGENCY_SESSION', ?, ?)
                """, eventId, userId, Timestamp.from(createdAt), Timestamp.from(createdAt),
                Timestamp.from(createdAt));
    }

    private void seedRecipientDevice(UUID recipientId, UUID deviceId, String token) {
        jdbcTemplate.update("""
                INSERT INTO users(
                    user_id,person_id,display_name,email,role,account_status,enabled,locked,
                    email_verified,phone_verified,created_at,updated_at)
                VALUES (?, ?, 'Restart Recipient', ?, 'FAMILY', 'ACTIVE', true, false,
                        true, false, now(), now())
                """, recipientId, recipientId, "restart-recipient-" + recipientId + "@test");
        jdbcTemplate.update("""
                INSERT INTO device_tokens(id,user_id,token,platform,active,created_at,updated_at)
                VALUES (?, ?, ?, 'ANDROID', true, now(), now())
                """, deviceId, recipientId, token);
    }

    private String alertStatus(UUID sessionId) {
        return jdbcTemplate.queryForObject("""
                SELECT alert_status FROM safety_events WHERE safety_event_id=?
                """, String.class, sessionId);
    }

    private long deliveryResultCount(UUID sessionId, UUID deviceId, String status) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE parent_event_id=? AND device_token_id=?
                   AND action_type='DELIVERY' AND action_phase='RESULT'
                   AND delivery_status=?
                """, Long.class, sessionId, deviceId, status);
    }

    private void assertEscalationCountersZero(UUID intakeId) {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM safety_events
                 WHERE user_id = ? AND record_type = 'SAFETY_ACTION'
                   AND action_type = 'TRIAGE_ESCALATION'
                   AND triage_handoff_id = ?
                   AND alert_successful_recipient_count = 0
                   AND alert_failed_recipient_count = 0
                """, Long.class, ownerId, intakeId))
                .isOne();
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
