package com.carebridge.backend.safety;

import com.carebridge.backend.safety.dto.response.ImuMonitoringSessionResponse;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.event.FallDetectionDisabled;
import com.carebridge.backend.safety.event.FallDetectionEnabled;
import com.carebridge.backend.safety.event.SuspectedFallDetected;
import com.carebridge.backend.safety.repository.IImuMonitoringSessionRepository;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import com.carebridge.backend.safety.repository.SafetyConfigStore;
import com.carebridge.backend.safety.repository.SafetyEventResponseRepository;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.policy.SafetyConsentPolicy;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.service.IEmergencyService;
import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.safety.service.IFallDetectionAlgorithmService;
import com.carebridge.backend.safety.service.FallAnalysisResult;
import com.carebridge.backend.safety.service.ImuDataPayload;
import com.carebridge.backend.safety.service.SafetyCountdownTransactionRunner;
import com.carebridge.backend.safety.exception.SafetyException;
import com.carebridge.backend.safety.service.impl.FallDetectionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallDetectionServiceTest {

    @Mock
    private IImuMonitoringSessionRepository imuSessionRepository;

    @Mock
    private ISafetyEventRepository safetyEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock private SafetyConfigStore safetyConfigStore;
    @Mock private SafetyEventResponseRepository responseRepository;
    @Mock private SafetyConsentPolicy consentPolicy;
    @Mock private IEmergencyService emergencyService;
    @Mock private AuditService auditService;
    @Mock private IFallDetectionAlgorithmService algorithmService;
    @Mock private SafetyCountdownTransactionRunner countdownTransactionRunner;

    @InjectMocks
    private FallDetectionService fallDetectionService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private SafetyMonitoringConfig config;

    @BeforeEach
    void canonicalSafetyDefaults() {
        config = SafetyMonitoringConfig.builder()
                .userId(USER_ID)
                .fallDetectionEnabled(true)
                .sensitivityLevel(SensitivityLevel.MEDIUM)
                .emergencyAutoAlert(true)
                .countdownSeconds(30)
                .sensorPermissionGranted(true)
                .sensorPermissionRecordedAt(Instant.now())
                .build();
        lenient().when(safetyConfigStore.findByUserId(USER_ID)).thenReturn(Optional.of(config));
        lenient().when(responseRepository.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(countdownTransactionRunner).run(any(Runnable.class));
        lenient().when(emergencyService.openFlow(any(), eq(USER_ID))).thenReturn(
                EmergencySessionResponse.builder().sessionId(UUID.randomUUID()).userId(USER_ID).build());
    }

    @Test
    void enable_noActiveSession_shouldCreateNew() {
        // FD-TC-001
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(imuSessionRepository.save(any())).thenReturn(SafetyConfigTestFactory.makeActiveSession());

        ImuMonitoringSessionResponse result = fallDetectionService.enable(USER_ID, "MEDIUM");

        verify(imuSessionRepository).save(any(ImuMonitoringSession.class));
        InOrder enableOrder = inOrder(imuSessionRepository);
        enableOrder.verify(imuSessionRepository).acquireUserLock(USER_ID);
        enableOrder.verify(imuSessionRepository).findActiveByUserId(USER_ID);
        enableOrder.verify(imuSessionRepository).save(any(ImuMonitoringSession.class));
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void enable_activeSessionExists_shouldReturnExisting() {
        // FD-TC-002 — idempotent
        ImuMonitoringSession existing = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        ImuMonitoringSessionResponse result = fallDetectionService.enable(USER_ID, "MEDIUM");

        verify(imuSessionRepository, never()).save(any());
        assertThat(result.getSessionId()).isEqualTo(existing.getId());
    }

    @Test
    void enable_shouldPublishFallDetectionEnabledEvent() {
        // FD-TC-004
        when(imuSessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(imuSessionRepository.save(any())).thenReturn(SafetyConfigTestFactory.makeActiveSession());

        fallDetectionService.enable(USER_ID, "MEDIUM");

        ArgumentCaptor<FallDetectionEnabled> captor = ArgumentCaptor.forClass(FallDetectionEnabled.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(FallDetectionEnabled.class);
    }

    @Test
    void disable_activeSessionExists_shouldSetStatusStopped() {
        // DIS-TC-001 / FD-TC-005
        ImuMonitoringSession active = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(active));
        when(imuSessionRepository.save(any())).thenReturn(active);

        fallDetectionService.disable(USER_ID);

        InOrder disableOrder = inOrder(imuSessionRepository);
        disableOrder.verify(imuSessionRepository).acquireUserLock(USER_ID);
        disableOrder.verify(imuSessionRepository).findActiveForUpdateByUserId(USER_ID);
        disableOrder.verify(imuSessionRepository).save(any(ImuMonitoringSession.class));
        ArgumentCaptor<ImuMonitoringSession> captor = ArgumentCaptor.forClass(ImuMonitoringSession.class);
        verify(imuSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ImuSessionStatus.STOPPED);
        assertThat(captor.getValue().getEndedAt()).isNotNull();
        verify(imuSessionRepository, never()).delete(any());
    }

    @Test
    void disable_noActiveSession_shouldBeNoOp() {
        // DIS-TC-002
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatCode(() -> fallDetectionService.disable(USER_ID)).doesNotThrowAnyException();
        verify(imuSessionRepository, never()).save(any());
    }

    @Test
    void disable_shouldPublishFallDetectionDisabledEvent() {
        // DIS-TC-004
        ImuMonitoringSession active = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(active));
        when(imuSessionRepository.save(any())).thenReturn(active);

        fallDetectionService.disable(USER_ID);

        verify(eventPublisher).publishEvent(any(FallDetectionDisabled.class));
    }

    @Test
    void listSafetyEvents_shouldReturnUserEventsNewestFirst() {
        SafetyEvent event = makeSafetyEvent();
        when(safetyEventRepository.findByUserIdOrderByDetectedAtDesc(eq(USER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(event)));

        List<SafetyEventResponse> result = fallDetectionService.listSafetyEvents(USER_ID, PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(event.getId());
        verify(safetyEventRepository).findByUserIdOrderByDetectedAtDesc(eq(USER_ID), any());
    }

    @Test
    void confirmSafetyCheck_shouldMarkEventSafe() {
        SafetyEvent event = makeSafetyEvent();
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SafetyEventResponse result = fallDetectionService.confirmSafetyCheck(USER_ID, event.getId(), "Baby is safe");

        assertThat(result.getStatus()).isEqualTo("CONFIRMED_SAFE");
        assertThat(event.getNotes()).contains("Baby is safe");
    }

    @Test
    void confirmSafetyCheck_shouldMarkSensorSelfTestEventSafe() {
        SafetyEvent event = makeSafetyEvent();
        event.setEventType(SafetyEventType.SENSOR_SELF_TEST);
        event.setStatus(SafetyEventStatus.TEST_OPEN);
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SafetyEventResponse result = fallDetectionService.confirmSafetyCheck(USER_ID, event.getId(), "Diễn tập safe");

        assertThat(result.getStatus()).isEqualTo("CONFIRMED_SAFE");
        assertThat(event.getResponseType()).isEqualTo("I_AM_OK");
    }

    @Test
    void reportFalsePositive_shouldMarkEventFalsePositive() {
        SafetyEvent event = makeSafetyEvent();
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SafetyEventResponse result = fallDetectionService.reportFalsePositive(USER_ID, event.getId(), "Phone dropped");

        assertThat(result.getStatus()).isEqualTo("FALSE_POSITIVE");
        assertThat(event.getNotes()).contains("Phone dropped");
    }

    @Test
    void reportFalsePositive_shouldAllowOverridingTimedOutEvent() {
        SafetyEvent event = makeSafetyEvent();
        event.setResponseType("TIMEOUT");
        event.setStatus(SafetyEventStatus.TIMED_OUT);
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SafetyEventResponse result = fallDetectionService.reportFalsePositive(USER_ID, event.getId(), "User reacted late");

        assertThat(result.getStatus()).isEqualTo("FALSE_POSITIVE");
        assertThat(event.getResponseType()).isEqualTo("FALSE_POSITIVE");
    }

    @Test
    void reportFalsePositive_preservesResponseReasonAndCapsCanonicalNote() {
        SafetyEvent event = makeSafetyEvent();
        String reason = "x".repeat(500);
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fallDetectionService.reportFalsePositive(USER_ID, event.getId(), reason);

        assertThat(event.getNotes()).hasSize(255);
        assertThat(event.getResponseReason()).isEqualTo(reason);
    }

    @Test
    void sendEmergencyAlert_shouldOpenEmergencyWithoutRepublishingDetectionEvent() {
        SafetyEvent event = makeSafetyEvent();
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        verify(emergencyService).openFlow(any(), eq(USER_ID));
        verify(eventPublisher, never()).publishEvent(any(SuspectedFallDetected.class));
    }

    @Test
    void sendEmergencyAlert_sensorSelfTestShouldNeverOpenEmergency() {
        SafetyEvent event = makeSafetyEvent();
        event.setEventType(SafetyEventType.SENSOR_SELF_TEST);
        event.setStatus(SafetyEventStatus.TEST_OPEN);
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> fallDetectionService.sendEmergencyAlert(USER_ID, event.getId()))
                .isInstanceOf(SafetyException.class)
                .hasMessageContaining("cannot trigger emergency alerts");

        verify(emergencyService, never()).openFlow(any(), any());
        verify(responseRepository, never()).insert(any());
    }

    @Test
    void sendEmergencyAlert_afterSafeResponseIsIdempotent() {
        SafetyEvent event = makeSafetyEvent();
        event.setResponseType("I_AM_OK");
        event.setStatus(SafetyEventStatus.CONFIRMED_SAFE);
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        verify(emergencyService, never()).openFlow(any(), any());
        verify(responseRepository, never()).insert(any());
    }

    @Test
    void sendEmergencyAlert_afterFalsePositiveResponseIsIdempotent() {
        SafetyEvent event = makeSafetyEvent();
        event.setResponseType("FALSE_POSITIVE");
        event.setStatus(SafetyEventStatus.FALSE_POSITIVE);
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        verify(emergencyService, never()).openFlow(any(), any());
        verify(responseRepository, never()).insert(any());
    }

    @Test
    void sendEmergencyAlert_sameResponseTwiceOpensOnlyOneEmergency() {
        SafetyEvent event = makeSafetyEvent();
        UUID emergencySessionId = UUID.randomUUID();
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emergencyService.openFlow(any(), eq(USER_ID))).thenReturn(
                EmergencySessionResponse.builder().sessionId(emergencySessionId).userId(USER_ID).build());

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());
        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        verify(safetyEventRepository, times(4)).findLockedByIdAndUserId(event.getId(), USER_ID);
        verify(emergencyService, times(1)).openFlow(any(), eq(USER_ID));
        verify(eventPublisher, never()).publishEvent(any(SuspectedFallDetected.class));
    }

    @Test
    void sendEmergencyAlert_timeoutResponseContinuesEmergencyEscalation() {
        SafetyEvent event = makeSafetyEvent();
        event.setResponseType("TIMEOUT");
        event.setStatus(SafetyEventStatus.TIMED_OUT);
        event.setResolvedAt(Instant.now());
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.ESCALATION_REQUESTED);
        assertThat(event.getResolvedAt()).isNull();
        verify(emergencyService).openFlow(any(), eq(USER_ID));
        verify(responseRepository, never()).insert(any());
    }

    @Test
    void sendEmergencyAlert_reusedSentSessionSynchronizesLinkedImuEventWithoutRepublishing() {
        SafetyEvent event = makeSafetyEvent();
        UUID emergencySessionId = UUID.randomUUID();
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emergencyService.openFlow(any(), eq(USER_ID))).thenReturn(
                EmergencySessionResponse.builder()
                        .sessionId(emergencySessionId)
                        .userId(USER_ID)
                        .build());
        when(safetyEventRepository.transitionLinkedEventForSentEmergencySession(
                event.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT))
                .thenReturn(1);

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        assertThat(event.getEmergencySessionId()).isEqualTo(emergencySessionId);
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.EMERGENCY_ALERT_SENT);
        verify(safetyEventRepository).transitionLinkedEventForSentEmergencySession(
                event.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT);
        verify(eventPublisher, never()).publishEvent(any(SuspectedFallDetected.class));
    }

    @Test
    void sendEmergencyAlert_replayedLinkedEventSynchronizesWithoutOpeningAnotherEmergency() {
        SafetyEvent event = makeSafetyEvent();
        event.setEmergencySessionId(UUID.randomUUID());
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID))
                .thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(safetyEventRepository.transitionLinkedEventForSentEmergencySession(
                event.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT))
                .thenReturn(1);

        fallDetectionService.sendEmergencyAlert(USER_ID, event.getId());

        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.EMERGENCY_ALERT_SENT);
        verify(emergencyService, never()).openFlow(any(), eq(USER_ID));
        verify(safetyEventRepository).transitionLinkedEventForSentEmergencySession(
                event.getId(),
                SafetyEventStatus.ESCALATION_REQUESTED,
                SafetyEventStatus.EMERGENCY_ALERT_SENT);
    }

    @Test
    void enable_deniedSensorPermissionDoesNotCreateActiveSession() {
        config.setSensorPermissionGranted(false);

        assertThatThrownBy(() -> fallDetectionService.enable(USER_ID, "MEDIUM"))
                .isInstanceOf(SafetyException.class)
                .extracting("code").isEqualTo("SAFETY-009");
        verify(imuSessionRepository, never()).save(any());
    }

    @Test
    void processImuData_duplicateSignalReturnsExistingWithoutSecondAnalysis() {
        ImuMonitoringSession session = SafetyConfigTestFactory.makeActiveSession();
        SafetyEvent existing = makeSafetyEvent();
        existing.setSignalKey("signal-1");
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(session.getId(), "signal-1"))
                .thenReturn(Optional.of(existing));

        SafetyEventResponse result = fallDetectionService.processImuData(USER_ID, payload("signal-1"));

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(algorithmService, never()).analyze(any(), anyString());
        verify(safetyEventRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(SuspectedFallDetected.class));
    }

    @Test
    void processImuData_withoutLocationConsentDoesNotPersistCoordinates() {
        ImuMonitoringSession session = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(any(), anyString())).thenReturn(Optional.empty());
        when(algorithmService.analyze(any(), anyString()))
                .thenReturn(new FallAnalysisResult(true, SafetyEventType.SUSPECTED_FALL, 12.4));
        when(consentPolicy.mayPersistLocation(USER_ID)).thenReturn(false);
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> {
            SafetyEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        fallDetectionService.processImuData(USER_ID, payload("signal-2"));

        verify(safetyEventRepository).save(argThat(event ->
                event.getUserLatitude() == null && event.getUserLongitude() == null
                        && event.getCountdownDeadlineAt() != null));
    }

    @Test
    void processImuData_suspectedEventPublishesDetectionExactlyOnceAfterOpenEventIsSaved() {
        ImuMonitoringSession session = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(any(), anyString())).thenReturn(Optional.empty());
        when(algorithmService.analyze(any(), anyString()))
                .thenReturn(new FallAnalysisResult(true, SafetyEventType.SUSPECTED_FALL, 15.2));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> {
            SafetyEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        SafetyEventResponse response = fallDetectionService.processImuData(USER_ID, payload("confirmed-candidate"));

        ArgumentCaptor<SuspectedFallDetected> eventCaptor =
                ArgumentCaptor.forClass(SuspectedFallDetected.class);
        InOrder order = inOrder(safetyEventRepository, eventPublisher);
        order.verify(safetyEventRepository).save(any(SafetyEvent.class));
        order.verify(eventPublisher).publishEvent(eventCaptor.capture());
        verify(eventPublisher, times(1)).publishEvent(any(SuspectedFallDetected.class));
        assertThat(eventCaptor.getValue().safetyEventId()).isEqualTo(response.getId());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo("SUSPECTED_FALL");
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void processImuData_onDeviceConfirmedSoftFallBypassesLegacyHardImpactThreshold() {
        ImuMonitoringSession session = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(any(), anyString())).thenReturn(Optional.empty());
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> {
            SafetyEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        SafetyEventResponse response = fallDetectionService.processImuData(USER_ID,
                new ImuDataPayload(9.6, 0, 0, 0.2, 0, 0, Instant.now(),
                        "soft-surface-candidate", null, null, true));

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo("SUSPECTED_FALL");
        verify(algorithmService, never()).analyze(any(), anyString());
    }

    @Test
    void expiredCountdownPersistsTimeoutAndOpensEmergencyOnce() {
        SafetyEvent event = makeSafetyEvent();
        event.setCountdownDeadlineAt(Instant.now().minusSeconds(1));
        when(safetyEventRepository
                .findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(
                        eq(SafetyEventStatus.OPEN), any())).thenReturn(List.of(event));
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fallDetectionService.processExpiredCountdowns();

        assertThat(event.getResponseType()).isEqualTo("TIMEOUT");
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.ESCALATION_REQUESTED);
        verify(emergencyService).openFlow(any(), eq(USER_ID));
        verify(responseRepository).insert(argThat(response ->
                "SYSTEM".equals(response.getActorType()) && response.getCreatedBy() == null));
    }

    @Test
    void expiredCountdownWithoutAutoAlertStillPersistsTerminalTimeout() {
        config.setEmergencyAutoAlert(false);
        SafetyEvent event = makeSafetyEvent();
        event.setCountdownDeadlineAt(Instant.now().minusSeconds(1));
        when(safetyEventRepository
                .findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(
                        eq(SafetyEventStatus.OPEN), any())).thenReturn(List.of(event));
        when(safetyEventRepository.findLockedByIdAndUserId(event.getId(), USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fallDetectionService.processExpiredCountdowns();

        assertThat(event.getResponseType()).isEqualTo("TIMEOUT");
        assertThat(event.getStatus()).isEqualTo(SafetyEventStatus.TIMED_OUT);
        assertThat(event.getResolvedAt()).isNotNull();
        verify(emergencyService, never()).openFlow(any(), any());
    }

    @Test
    void poisonCountdownRollsBackItsEventAndDoesNotBlockLaterEvents() {
        SafetyEvent poison = makeSafetyEvent();
        poison.setId(UUID.randomUUID());
        poison.setCountdownDeadlineAt(Instant.now().minusSeconds(2));
        SafetyEvent healthy = makeSafetyEvent();
        healthy.setId(UUID.randomUUID());
        healthy.setCountdownDeadlineAt(Instant.now().minusSeconds(1));
        when(safetyEventRepository
                .findTop100ByStatusAndResponseTypeIsNullAndCountdownDeadlineAtLessThanEqualOrderByCountdownDeadlineAtAsc(
                        eq(SafetyEventStatus.OPEN), any())).thenReturn(List.of(poison, healthy));
        when(safetyEventRepository.findLockedByIdAndUserId(poison.getId(), USER_ID))
                .thenThrow(new IllegalStateException("poison"));
        when(safetyEventRepository.findLockedByIdAndUserId(healthy.getId(), USER_ID))
                .thenReturn(Optional.of(healthy));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        fallDetectionService.processExpiredCountdowns();

        assertThat(poison.getResponseType()).isNull();
        assertThat(healthy.getResponseType()).isEqualTo("TIMEOUT");
        verify(countdownTransactionRunner, times(2)).run(any(Runnable.class));
        verify(emergencyService, times(1)).openFlow(any(), eq(USER_ID));
    }

    @Test
    void processImuDataRejectsTimestampOutsideAcceptedSkew() {
        ImuMonitoringSession session = SafetyConfigTestFactory.makeActiveSession();
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
        ImuDataPayload stale = new ImuDataPayload(1, 2, 3, 4, 5, 6,
                Instant.now().minusSeconds(25 * 60 * 60), "stale", null, null, false);

        assertThatThrownBy(() -> fallDetectionService.processImuData(USER_ID, stale))
                .isInstanceOf(SafetyException.class)
                .extracting("code").isEqualTo("SAFETY-012");
        verify(algorithmService, never()).analyze(any(), anyString());
    }

    @Test
    void processImuDataUsesServerDetectedAtAndRetainsValidatedClientTime() {
        ImuMonitoringSession session = SafetyConfigTestFactory.makeActiveSession();
        Instant clientTime = Instant.now().minusSeconds(30);
        when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(any(), anyString())).thenReturn(Optional.empty());
        when(algorithmService.analyze(any(), anyString()))
                .thenReturn(new FallAnalysisResult(true, SafetyEventType.SUSPECTED_FALL, 12.4));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> {
            SafetyEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        SafetyEventResponse response = fallDetectionService.processImuData(USER_ID,
                new ImuDataPayload(1, 2, 3, 4, 5, 6, clientTime, "ordered", null, null, false));

        assertThat(response.getClientDetectedAt()).isEqualTo(clientTime);
        assertThat(response.getDetectedAt()).isAfter(clientTime);
    }

    private ImuDataPayload payload(String signalId) {
        return new ImuDataPayload(1, 2, 3, 4, 5, 6, Instant.now(), signalId,
                new BigDecimal("10.123"), new BigDecimal("106.456"), false);
    }

    private SafetyEvent makeSafetyEvent() {
        return SafetyEvent.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .userId(USER_ID)
                .imuSessionId(UUID.randomUUID())
                .eventType(SafetyEventType.SUSPECTED_FALL)
                .magnitude(BigDecimal.valueOf(12.4))
                .detectedAt(Instant.now())
                .createdBy("SYSTEM")
                .build();
    }
}
