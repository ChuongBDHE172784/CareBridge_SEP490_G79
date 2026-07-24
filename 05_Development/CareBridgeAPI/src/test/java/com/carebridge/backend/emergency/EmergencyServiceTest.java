package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.dto.response.EmergencySessionResponse;
import com.carebridge.backend.emergency.dto.response.FamilyAlertDetailResponse;
import com.carebridge.backend.emergency.entity.EmergencySession;
import com.carebridge.backend.emergency.entity.EmergencyNotificationOutbox;
import com.carebridge.backend.emergency.entity.TriageEmergencyEscalation;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencySessionRepository;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.repository.EmergencyNotificationOutboxRepository;
import com.carebridge.backend.emergency.repository.TriageEmergencyEscalationRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.impl.EmergencyService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.entity.IntakeSession;
import com.carebridge.backend.triage.repository.IIntakeSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyServiceTest {

    @Mock
    private IEmergencySessionRepository emergencySessionRepository;

    @Mock
    private TriageEmergencyEscalationRepository triageEmergencyEscalationRepository;

    @Mock
    private EmergencyNotificationOutboxRepository emergencyNotificationOutboxRepository;

    @Mock
    private IIntakeSessionRepository intakeSessionRepository;

    @Mock
    private IFamilyAlertLogRepository familyAlertLogRepository;

    @Mock
    private FamilyMemberPort familyMemberPort;

    @Mock
    private LocationConsentPort locationConsentPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EmergencyService emergencyService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID INTAKE_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUpAuthoritativeRedIntake() {
        lenient().when(intakeSessionRepository.findByIdAndUserId(INTAKE_SESSION_ID, USER_ID))
                .thenReturn(Optional.of(completedIntake(USER_ID, RiskLevel.RED)));
    }

    @Test
    void openFlow_noActiveSession_shouldCreateNew() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        EmergencySession saved = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.save(any())).thenReturn(saved);

        EmergencySessionResponse result = emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verify(emergencySessionRepository).save(any(EmergencySession.class));
        verify(emergencySessionRepository).acquireUserLock(USER_ID);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void openFlow_activeSessionExists_shouldReturnExisting() {
        // Idempotent — return existing ACTIVE session
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        EmergencySessionResponse result = emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verify(emergencySessionRepository, never()).save(any());
        verify(emergencySessionRepository).acquireUserLock(USER_ID);
        assertThat(result.getSessionId()).isEqualTo(existing.getId());
    }

    @Test
    void openFlow_shouldPublishEmergencySessionOpenedEvent() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(emergencySessionRepository.save(any())).thenReturn(EmergencyTestFactory.makeActiveSession());

        emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        ArgumentCaptor<EmergencySessionOpened> captor = ArgumentCaptor.forClass(EmergencySessionOpened.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(EmergencySessionOpened.class);
    }

    @Test
    void resolveSession_shouldImmediatelySuppressPendingNotificationClaim() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        EmergencyNotificationOutbox outbox = EmergencyNotificationOutbox.builder()
                .emergencySessionId(session.getId())
                .status(EmergencyNotificationOutbox.PENDING)
                .attemptCount(1)
                .nextAttemptAt(Instant.now().plusSeconds(30))
                .claimToken(UUID.randomUUID())
                .createdAt(Instant.now())
                .build();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(emergencyNotificationOutboxRepository.findForUpdate(session.getId()))
                .thenReturn(Optional.of(outbox));
        when(emergencySessionRepository.save(session)).thenReturn(session);

        EmergencySessionResponse response = emergencyService.resolveSession(session.getId(), USER_ID);

        assertThat(response.getStatus()).isEqualTo(EmergencyStatus.RESOLVED.name());
        assertThat(outbox.getStatus()).isEqualTo(EmergencyNotificationOutbox.SUPPRESSED);
        assertThat(outbox.getLastErrorCode()).isEqualTo("EMERGENCY_NOT_ACTIVE");
        assertThat(outbox.getClaimToken()).isNull();
        assertThat(outbox.getTerminalAt()).isNotNull();
        verify(emergencySessionRepository).acquireUserLock(USER_ID);
    }

    @Test
    void openOrReuseFromTriage_sameIntakeReplay_shouldReturnDurablyLinkedSessionWithoutOpeningAgain() {
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        TriageEmergencyEscalation link = TriageEmergencyEscalation.builder()
                .intakeSessionId(INTAKE_SESSION_ID)
                .emergencySessionId(existing.getId())
                .userId(USER_ID)
                .triggeredAt(Instant.parse("2026-07-22T03:00:00Z"))
                .build();
        when(triageEmergencyEscalationRepository.findByIntakeSessionId(INTAKE_SESSION_ID))
                .thenReturn(Optional.of(link));
        when(emergencySessionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        EmergencySessionResponse result = emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID);

        assertThat(result.getSessionId()).isEqualTo(existing.getId());
        verify(emergencySessionRepository).acquireUserLock(USER_ID);
        verify(emergencySessionRepository, never()).findActiveByUserId(any());
        verify(emergencySessionRepository, never()).save(any());
        verify(triageEmergencyEscalationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(EmergencySessionOpened.class));
    }

    @Test
    void openOrReuseFromTriage_existingActiveSession_shouldAssociateIntakeWithoutOpeningAgain() {
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        when(triageEmergencyEscalationRepository.findByIntakeSessionId(INTAKE_SESSION_ID))
                .thenReturn(Optional.empty());
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(triageEmergencyEscalationRepository.save(any(TriageEmergencyEscalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmergencySessionResponse result = emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID);

        assertThat(result.getSessionId()).isEqualTo(existing.getId());
        ArgumentCaptor<TriageEmergencyEscalation> linkCaptor =
                ArgumentCaptor.forClass(TriageEmergencyEscalation.class);
        verify(triageEmergencyEscalationRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getIntakeSessionId()).isEqualTo(INTAKE_SESSION_ID);
        assertThat(linkCaptor.getValue().getEmergencySessionId()).isEqualTo(existing.getId());
        assertThat(linkCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        verify(emergencySessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(EmergencySessionOpened.class));
    }

    @Test
    void openOrReuseFromTriage_noAssociationOrActiveSession_shouldCreateAndOpenExactlyOnce() {
        EmergencySession saved = EmergencyTestFactory.makeActiveSession();
        when(triageEmergencyEscalationRepository.findByIntakeSessionId(INTAKE_SESSION_ID))
                .thenReturn(Optional.empty());
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(emergencySessionRepository.save(any(EmergencySession.class))).thenReturn(saved);
        when(triageEmergencyEscalationRepository.save(any(TriageEmergencyEscalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmergencySessionResponse result = emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID);

        assertThat(result.getSessionId()).isEqualTo(saved.getId());
        verify(emergencySessionRepository).save(any(EmergencySession.class));
        verify(triageEmergencyEscalationRepository).save(any(TriageEmergencyEscalation.class));
        verify(eventPublisher, times(1)).publishEvent(any(EmergencySessionOpened.class));
    }

    @Test
    void openOrReuseFromTriage_shouldAcquireOwnerLockBeforeAssociationAndActiveLookups() {
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        when(triageEmergencyEscalationRepository.findByIntakeSessionId(INTAKE_SESSION_ID))
                .thenReturn(Optional.empty());
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(triageEmergencyEscalationRepository.save(any(TriageEmergencyEscalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID);

        InOrder order = inOrder(emergencySessionRepository, triageEmergencyEscalationRepository);
        order.verify(emergencySessionRepository).acquireUserLock(USER_ID);
        order.verify(triageEmergencyEscalationRepository).findByIntakeSessionId(INTAKE_SESSION_ID);
        order.verify(emergencySessionRepository).findActiveByUserId(USER_ID);
    }

    @Test
    void openOrReuseFromTriage_missingIntake_shouldRejectBeforeAssociationOrEmergencyMutation() {
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_SESSION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID))
                .isInstanceOfSatisfying(EmergencyException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EMERG-006"));

        verifyNoInteractions(triageEmergencyEscalationRepository, eventPublisher);
        verify(emergencySessionRepository, never()).findActiveByUserId(any());
        verify(emergencySessionRepository, never()).save(any());
    }

    @Test
    void openOrReuseFromTriage_foreignIntake_shouldRejectWithoutDisclosingOrLinkingIt() {
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_SESSION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID))
                .isInstanceOfSatisfying(EmergencyException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EMERG-006"));

        verifyNoInteractions(triageEmergencyEscalationRepository, eventPublisher);
        verify(emergencySessionRepository, never()).save(any());
    }

    @Test
    void openOrReuseFromTriage_nonCompletedIntake_shouldRejectBeforeCreatingEmergency() {
        IntakeSession processing = completedIntake(USER_ID, RiskLevel.RED);
        processing.setStatus(IntakeStatus.PROCESSING);
        processing.setCompletedAt(null);
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_SESSION_ID, USER_ID))
                .thenReturn(Optional.of(processing));

        assertThatThrownBy(() -> emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID))
                .isInstanceOfSatisfying(EmergencyException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EMERG-006"));

        verifyNoInteractions(triageEmergencyEscalationRepository, eventPublisher);
        verify(emergencySessionRepository, never()).save(any());
    }

    @Test
    void openOrReuseFromTriage_nonRedIntake_shouldRejectBeforeCreatingEmergency() {
        when(intakeSessionRepository.findByIdAndUserId(INTAKE_SESSION_ID, USER_ID))
                .thenReturn(Optional.of(completedIntake(USER_ID, RiskLevel.YELLOW)));

        assertThatThrownBy(() -> emergencyService.openOrReuseFromTriage(INTAKE_SESSION_ID, USER_ID))
                .isInstanceOfSatisfying(EmergencyException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EMERG-006"));

        verifyNoInteractions(triageEmergencyEscalationRepository, eventPublisher);
        verify(emergencySessionRepository, never()).save(any());
    }

    @Test
    void openFlow_newSession_shouldRecordPendingNotificationOutboxInCreationTransaction() {
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        EmergencySession saved = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.save(any(EmergencySession.class))).thenReturn(saved);
        when(emergencyNotificationOutboxRepository.save(any(EmergencyNotificationOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        ArgumentCaptor<EmergencyNotificationOutbox> captor =
                ArgumentCaptor.forClass(EmergencyNotificationOutbox.class);
        verify(emergencyNotificationOutboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEmergencySessionId()).isEqualTo(saved.getId());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmergencyNotificationOutbox.PENDING);
        assertThat(captor.getValue().getAttemptCount()).isZero();
    }

    @Test
    void openFlow_reusedSession_shouldNotCreateDuplicateNotificationOutbox() {
        EmergencySession existing = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findActiveByUserId(USER_ID)).thenReturn(Optional.of(existing));

        emergencyService.openFlow(EmergencyTestFactory.makeOpenRequest(), USER_ID);

        verifyNoInteractions(emergencyNotificationOutboxRepository, eventPublisher);
    }

    @Test
    void getAlertDetail_owner_shouldReturnDetailWithoutFamilyMembershipCheck() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().name("Mai").build()));
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());

        FamilyAlertDetailResponse result = emergencyService.getAlertDetail(session.getId(), USER_ID);

        assertThat(result.getMotherName()).isEqualTo("Mai");
        assertThat(result.getSessionId()).isEqualTo(session.getId());
        verify(familyMemberPort, never()).isFamilyMember(any(), any());
    }

    @Test
    void getAlertDetail_acceptedFamilyMember_shouldReturnDetail() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(familyMemberPort.isFamilyMember(USER_ID, FAMILY_ID)).thenReturn(true);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());

        FamilyAlertDetailResponse result = emergencyService.getAlertDetail(session.getId(), FAMILY_ID);

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isNull();
    }

    @Test
    void getAlertDetail_strangerNotInFamily_shouldThrowForbidden() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(familyMemberPort.isFamilyMember(USER_ID, STRANGER_ID)).thenReturn(false);

        assertThatThrownBy(() -> emergencyService.getAlertDetail(session.getId(), STRANGER_ID))
                .isInstanceOf(EmergencyException.class)
                .satisfies(e -> assertThat(((EmergencyException) e).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAlertDetail_noConsent_shouldHideLocation() {
        EmergencySession session = EmergencyTestFactory.makeActiveSession();
        session.setUserLatitude(java.math.BigDecimal.valueOf(10.77));
        session.setUserLongitude(java.math.BigDecimal.valueOf(106.70));
        when(emergencySessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(familyAlertLogRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());

        FamilyAlertDetailResponse result = emergencyService.getAlertDetail(session.getId(), USER_ID);

        assertThat(result.getLatitude()).isNull();
        assertThat(result.getLongitude()).isNull();
        assertThat(result.isLocationIncluded()).isFalse();
    }

    private IntakeSession completedIntake(UUID ownerUserId, RiskLevel riskLevel) {
        return IntakeSession.builder()
                .id(INTAKE_SESSION_ID)
                .userId(ownerUserId)
                .symptoms("SYNTHETIC_REDACTED")
                .stage(com.carebridge.backend.triage.TriageStage.INFANT)
                .riskLevel(riskLevel)
                .status(IntakeStatus.COMPLETED)
                .createdAt(Instant.parse("2026-07-22T03:00:00Z"))
                .completedAt(Instant.parse("2026-07-22T03:01:00Z"))
                .createdBy(ownerUserId)
                .build();
    }
}
