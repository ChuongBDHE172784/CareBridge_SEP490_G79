package com.carebridge.backend.safety;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.safety.dto.request.SensorSelfTestCompletionRequest;
import com.carebridge.backend.safety.dto.request.SensorSelfTestEventRequest;
import com.carebridge.backend.safety.dto.response.SafetyEventResponse;
import com.carebridge.backend.safety.entity.ImuMonitoringSession;
import com.carebridge.backend.safety.entity.SafetyEvent;
import com.carebridge.backend.safety.entity.SafetyMonitoringConfig;
import com.carebridge.backend.safety.policy.SafetyConsentPolicy;
import com.carebridge.backend.safety.repository.IImuMonitoringSessionRepository;
import com.carebridge.backend.safety.repository.ISafetyConfigRepository;
import com.carebridge.backend.safety.repository.ISafetyEventRepository;
import com.carebridge.backend.safety.repository.SafetyEventResponseRepository;
import com.carebridge.backend.safety.service.impl.SensorSelfTestService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorSelfTestServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");

    @Mock private IImuMonitoringSessionRepository imuSessionRepository;
    @Mock private ISafetyEventRepository safetyEventRepository;
    @Mock private ISafetyConfigRepository safetyConfigRepository;
    @Mock private SafetyEventResponseRepository responseRepository;
    @Mock private SafetyConsentPolicy consentPolicy;
    @Mock private AuditService auditService;

    @InjectMocks private SensorSelfTestService service;

    @BeforeEach
    void setUp() {
        SafetyMonitoringConfig config = SafetyMonitoringConfig.builder()
                .userId(USER_ID)
                .fallDetectionEnabled(true)
                .sensitivityLevel(SensitivityLevel.MEDIUM)
                .emergencyAutoAlert(true)
                .countdownSeconds(30)
                .sensorPermissionGranted(true)
                .build();
        ImuMonitoringSession session = ImuMonitoringSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .status(ImuSessionStatus.ACTIVE)
                .sensitivityLevel("MEDIUM")
                .startedAt(Instant.now())
                .build();
        lenient().when(safetyConfigRepository.findByUserId(USER_ID)).thenReturn(Optional.of(config));
        lenient().when(imuSessionRepository.findActiveForUpdateByUserId(USER_ID)).thenReturn(Optional.of(session));
    }

    @Test
    void create_persistsIsolatedTestOpenEventAndIsIdempotent() {
        SensorSelfTestEventRequest request = SensorSelfTestEventRequest.builder()
                .testId("gesture-1")
                .detectedAt(Instant.now())
                .accelerationMagnitude(17.2)
                .gyroscopeMagnitude(3.4)
                .build();
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(SESSION_ID, "SELF_TEST:gesture-1"))
                .thenReturn(Optional.empty());
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> {
            SafetyEvent event = invocation.getArgument(0);
            event.setId(EVENT_ID);
            return event;
        });

        SafetyEventResponse response = service.create(USER_ID, request);

        ArgumentCaptor<SafetyEvent> eventCaptor = ArgumentCaptor.forClass(SafetyEvent.class);
        verify(safetyEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(SafetyEventType.SENSOR_SELF_TEST);
        assertThat(eventCaptor.getValue().getStatus()).isEqualTo(SafetyEventStatus.TEST_OPEN);
        assertThat(eventCaptor.getValue().getCountdownDeadlineAt()).isNotNull();
        assertThat(response.getEventType()).isEqualTo("SENSOR_SELF_TEST");
        assertThat(response.getStatus()).isEqualTo("TEST_OPEN");
    }

    @Test
    void create_duplicateReturnsExistingEventWithoutSecondInsert() {
        SafetyEvent existing = selfTestEvent();
        SensorSelfTestEventRequest request = SensorSelfTestEventRequest.builder()
                .testId("gesture-1")
                .detectedAt(Instant.now())
                .accelerationMagnitude(17.2)
                .gyroscopeMagnitude(3.4)
                .build();
        when(safetyEventRepository.findByImuSessionIdAndSignalKey(SESSION_ID, "SELF_TEST:gesture-1"))
                .thenReturn(Optional.of(existing));

        SafetyEventResponse response = service.create(USER_ID, request);

        assertThat(response.getId()).isEqualTo(EVENT_ID);
        verify(safetyEventRepository, never()).save(any());
    }

    @Test
    void completeNeedHelp_resolvesAsTestOnlyResponse() {
        SafetyEvent event = selfTestEvent();
        when(safetyEventRepository.findLockedByIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SafetyEventResponse response = service.complete(
                USER_ID, EVENT_ID, new SensorSelfTestCompletionRequest("NEED_HELP"));

        assertThat(response.getStatus()).isEqualTo("TIMED_OUT");
        assertThat(response.getResponseType()).isEqualTo("TEST_NEED_HELP");
        assertThat(response.getEmergencySessionId()).isNull();
        verify(responseRepository).insert(any());
    }

    @Test
    void completeTimeout_resolvesWithoutEmergencySession() {
        SafetyEvent event = selfTestEvent();
        when(safetyEventRepository.findLockedByIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(safetyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SafetyEventResponse response = service.complete(
                USER_ID, EVENT_ID, new SensorSelfTestCompletionRequest("TIMEOUT"));

        assertThat(response.getResponseType()).isEqualTo("TEST_TIMEOUT");
        assertThat(response.getResolvedAt()).isNotNull();
        assertThat(response.getEmergencySessionId()).isNull();
        verify(responseRepository).insert(any());
    }

    private SafetyEvent selfTestEvent() {
        return SafetyEvent.builder()
                .id(EVENT_ID)
                .userId(USER_ID)
                .imuSessionId(SESSION_ID)
                .eventType(SafetyEventType.SENSOR_SELF_TEST)
                .magnitude(java.math.BigDecimal.valueOf(17.2))
                .detectedAt(Instant.now())
                .status(SafetyEventStatus.TEST_OPEN)
                .countdownDeadlineAt(Instant.now().plusSeconds(30))
                .createdBy("SYSTEM_SELF_TEST")
                .build();
    }
}
