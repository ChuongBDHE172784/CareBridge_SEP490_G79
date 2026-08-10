package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;
import com.carebridge.backend.emergency.dto.request.OpenEmergencyRequest;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.repository.EmergencyAlertAcknowledgementRepository;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationLinkRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.impl.EmergencyService;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyServiceTest {
    @Mock private IEmergencySessionRepository emergencySessionRepository;
    @Mock private IIntakeSessionRepository intakeSessionRepository;
    @Mock private TriageEmergencyEscalationLinkRepository triageEscalationLinkRepository;
    @Mock private IFamilyAlertLogRepository familyAlertLogRepository;
    @Mock private FamilyMemberPort familyMemberPort;
    @Mock private LocationConsentPort locationConsentPort;
    @Mock private UserRepository userRepository;
    @Mock private EmergencyAlertAcknowledgementRepository acknowledgementRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private EmergencyService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INTAKE_ID = UUID.randomUUID();

    @Test
    void familyAlertDetailUsesRealMotherPhoneLocationAndAcknowledgement() {
        UUID callerId = UUID.randomUUID();
        EmergencySession session = emergencySession(null);
        session.setTriggerSource("FALL_DETECTION");
        session.setUserLatitude(new java.math.BigDecimal("10.762622"));
        session.setUserLongitude(new java.math.BigDecimal("106.660172"));
        User mother = User.builder()
                .id(USER_ID)
                .name("Mother Test")
                .phone("0901234567")
                .build();
        Instant acknowledgedAt = Instant.parse("2026-08-10T11:00:00Z");
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(familyMemberPort.isFamilyMember(USER_ID, callerId)).thenReturn(true);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mother));
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());
        when(acknowledgementRepository.find(session.getId(), callerId))
                .thenReturn(new EmergencyAlertAcknowledgementRepository.AcknowledgementState(
                        true, true, acknowledgedAt));

        var response = service.getAlertDetail(session.getId(), callerId);

        assertThat(response.getMotherName()).isEqualTo("Mother Test");
        assertThat(response.getMotherPhone()).isEqualTo("0901234567");
        assertThat(response.getLatitude()).isEqualByComparingTo("10.762622");
        assertThat(response.getLongitude()).isEqualByComparingTo("106.660172");
        assertThat(response.isAcknowledged()).isTrue();
        assertThat(response.getAcknowledgedAt()).isEqualTo(acknowledgedAt);
    }

    @Test
    void triageReplayUsesCanonicalSafetyEventSourceIdentity() {
        EmergencySession existing = emergencySession(INTAKE_ID);
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.of(completedRedIntake()));
        when(triageEscalationLinkRepository.findEmergencySessionId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.of(existing.getId()));
        when(emergencySessionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var response = service.openOrReuseFromTriage(INTAKE_ID, USER_ID);

        assertThat(response.getSessionId()).isEqualTo(existing.getId());
        verify(emergencySessionRepository, never()).save(any());
        verify(emergencySessionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void existingActiveEmergencyPublishesThrottledRealertTrigger() {
        EmergencySession active = emergencySession(null);
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(active));

        var response = service.openFlow(OpenEmergencyRequest.builder()
                .triggerSource("FALL_DETECTION")
                .build(), USER_ID);

        assertThat(response.getSessionId()).isEqualTo(active.getId());
        verify(emergencySessionRepository, never()).save(any());
        verify(eventPublisher).publishEvent(argThat((Object event) -> event instanceof EmergencySessionRealertRequested
                && ((EmergencySessionRealertRequested) event).sessionId().equals(active.getId())));
    }

    @Test
    void triageCreatesCanonicalEmergencyEventAndPublishesDeliveryTrigger() {
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.of(completedRedIntake()));
        when(triageEscalationLinkRepository.findEmergencySessionId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(emergencySessionRepository.saveAndFlush(any(EmergencySession.class)))
                .thenAnswer(invocation -> {
                    EmergencySession session = invocation.getArgument(0);
                    session.setId(UUID.randomUUID());
                    return session;
                });
        when(triageEscalationLinkRepository.linkIfAbsent(
                eq(INTAKE_ID), any(UUID.class), eq(USER_ID), any(Instant.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        var response = service.openOrReuseFromTriage(INTAKE_ID, USER_ID);

        assertThat(response.getTriggerSource()).isEqualTo("AUTO_TRIAGE");
        verify(emergencySessionRepository).saveAndFlush(argThat(session ->
                INTAKE_ID.equals(session.getSourceEventId())));
        verify(triageEscalationLinkRepository).linkIfAbsent(
                eq(INTAKE_ID), eq(response.getSessionId()), eq(USER_ID), any(Instant.class));
        verify(eventPublisher).publishEvent(any(EmergencySessionOpened.class));
    }

    @Test
    void activeManualSessionIsLinkedToTriageWithoutDuplicateEmergency() {
        EmergencySession active = emergencySession(null);
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.of(completedRedIntake()));
        when(triageEscalationLinkRepository.findEmergencySessionId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(active));
        when(emergencySessionRepository.save(active)).thenReturn(active);
        when(triageEscalationLinkRepository.linkIfAbsent(
                eq(INTAKE_ID), eq(active.getId()), eq(USER_ID), any(Instant.class)))
                .thenReturn(active.getId());

        var response = service.openOrReuseFromTriage(INTAKE_ID, USER_ID);

        assertThat(response.getSessionId()).isEqualTo(active.getId());
        assertThat(active.getSourceEventId()).isEqualTo(INTAKE_ID);
        verify(triageEscalationLinkRepository).linkIfAbsent(
                eq(INTAKE_ID), eq(active.getId()), eq(USER_ID), any(Instant.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void twoRedIntakesReuseOneEmergencyButKeepTwoCanonicalLinksAndReplay() {
        UUID secondIntakeId = UUID.randomUUID();
        EmergencySession shared = emergencySession(INTAKE_ID);
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_ID, USER_ID))
                .thenReturn(Optional.of(completedRedIntake(INTAKE_ID)));
        when(intakeSessionRepository.findByIdAndUserId(secondIntakeId, USER_ID))
                .thenReturn(Optional.of(completedRedIntake(secondIntakeId)));
        when(triageEscalationLinkRepository.findEmergencySessionId(any(), eq(USER_ID)))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(shared.getId()));
        when(emergencySessionRepository.findActiveByUserId(USER_ID))
                .thenReturn(Optional.empty(), Optional.of(shared));
        when(emergencySessionRepository.saveAndFlush(any(EmergencySession.class))).thenReturn(shared);
        when(triageEscalationLinkRepository.linkIfAbsent(
                eq(INTAKE_ID), eq(shared.getId()), eq(USER_ID), any(Instant.class)))
                .thenReturn(shared.getId());
        when(triageEscalationLinkRepository.linkIfAbsent(
                eq(secondIntakeId), eq(shared.getId()), eq(USER_ID), any(Instant.class)))
                .thenReturn(shared.getId());
        when(emergencySessionRepository.findById(shared.getId())).thenReturn(Optional.of(shared));

        var first = service.openOrReuseFromTriage(INTAKE_ID, USER_ID);
        var second = service.openOrReuseFromTriage(secondIntakeId, USER_ID);
        var replay = service.openOrReuseFromTriage(secondIntakeId, USER_ID);

        assertThat(first.getSessionId()).isEqualTo(shared.getId());
        assertThat(second.getSessionId()).isEqualTo(shared.getId());
        assertThat(replay.getSessionId()).isEqualTo(shared.getId());
        verify(triageEscalationLinkRepository).linkIfAbsent(
                eq(INTAKE_ID), eq(shared.getId()), eq(USER_ID), any(Instant.class));
        verify(triageEscalationLinkRepository).linkIfAbsent(
                eq(secondIntakeId), eq(shared.getId()), eq(USER_ID), any(Instant.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencySessionOpened.class));
    }

    @Test
    void resolvingSessionSuppressesAndFencesInFlightAlertProjection() {
        EmergencySession session = emergencySession(null);
        session.setAlertStatus("PROCESSING");
        session.setAlertClaimToken(UUID.randomUUID());
        session.setAlertLeaseExpiresAt(Instant.now().plusSeconds(120));
        when(emergencySessionRepository.findByIdForUpdate(session.getId()))
                .thenReturn(Optional.of(session));
        when(emergencySessionRepository.save(session)).thenReturn(session);

        service.resolveSession(session.getId(), USER_ID);

        assertThat(session.getStatus()).isEqualTo(EmergencyStatus.RESOLVED);
        assertThat(session.getAlertStatus()).isEqualTo("SUPPRESSED");
        assertThat(session.getAlertLeaseExpiresAt()).isNull();
        assertThat(session.getAlertUpdatedAt()).isNotNull();
        verify(emergencySessionRepository).findByIdForUpdate(session.getId());
    }

    private IntakeSession completedRedIntake() {
        return completedRedIntake(INTAKE_ID);
    }

    private IntakeSession completedRedIntake(UUID intakeId) {
        return IntakeSession.builder()
                .id(intakeId)
                .userId(USER_ID)
                .status(IntakeStatus.COMPLETED)
                .riskLevel(RiskLevel.RED)
                .build();
    }

    private EmergencySession emergencySession(UUID sourceEventId) {
        return EmergencySession.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .sourceEventId(sourceEventId)
                .status(EmergencyStatus.ACTIVE)
                .triggerSource("MANUAL")
                .createdAt(Instant.now())
                .createdBy(USER_ID)
                .build();
    }
}
