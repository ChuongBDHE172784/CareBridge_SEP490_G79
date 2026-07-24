package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.FamilyAlertDeliveryOutcome;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.SmsFallbackPort;
import com.carebridge.backend.emergency.service.impl.FamilyAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FamilyAlertServiceTest {

    @Mock
    private IFamilyAlertLogRepository familyAlertLogRepository;

    @Mock
    private FamilyMemberPort familyMemberPort;

    @Mock
    private FcmNotificationPort fcmNotificationPort;

    @Mock
    private LocationConsentPort locationConsentPort;

    @Mock
    private SmsFallbackPort smsFallbackPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FamilyAlertService familyAlertService;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void sendAlert_shouldCommitItsDeliveryLogBeforeOutboxTerminalTransition() throws Exception {
        Transactional transactional = FamilyAlertService.class
                .getMethod("sendAlert", EmergencySessionOpened.class)
                .getAnnotation(Transactional.class);

        org.assertj.core.api.Assertions.assertThat(transactional).isNotNull();
        org.assertj.core.api.Assertions.assertThat(transactional.propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void sendAlert_alreadySent_shouldBeIdempotentNoOp() {
        // UC65 — idempotent: skip if alert already sent for session
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(true);

        FamilyAlertDeliveryOutcome outcome = familyAlertService.sendAlert(
                EmergencyTestFactory.makeEmergencySessionOpenedEvent());

        assertThat(outcome).isEqualTo(FamilyAlertDeliveryOutcome.ALREADY_SENT);
        verify(fcmNotificationPort, never()).sendBatch(anyList(), any());
    }

    @Test
    void sendAlert_noRecipients_shouldBeIntentionalNoOp() {
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of());

        FamilyAlertDeliveryOutcome outcome = familyAlertService.sendAlert(
                EmergencyTestFactory.makeEmergencySessionOpenedEvent());

        assertThat(outcome).isEqualTo(FamilyAlertDeliveryOutcome.NO_RECIPIENTS);
        verify(fcmNotificationPort, never()).sendBatch(anyList(), any());
        verify(smsFallbackPort, never()).sendFallback(any(), any(), any());
        verify(familyAlertLogRepository, never()).save(any());
    }

    @Test
    void sendAlert_noConsent_shouldNotIncludeLocation() {
        // UC65 — PDPA: no location when consent=false
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001"));

        EmergencySessionOpened eventWithLocation = new EmergencySessionOpened(
                UUID.randomUUID(), SESSION_ID, USER_ID, "MANUAL",
                new BigDecimal("10.123"), new BigDecimal("106.456"),
                Instant.now());

        familyAlertService.sendAlert(eventWithLocation);

        verify(fcmNotificationPort).sendBatch(anyList(),
                argThat(payload -> !payload.containsKey("latitude") && !payload.containsKey("longitude")));
    }

    @Test
    void sendAlert_withConsent_shouldIncludeLocation() {
        // UC65 — location included when consent=true
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(true);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001", "token-002"));

        EmergencySessionOpened eventWithLocation = new EmergencySessionOpened(
                UUID.randomUUID(), SESSION_ID, USER_ID, "MANUAL",
                new BigDecimal("10.123"), new BigDecimal("106.456"),
                Instant.now());

        familyAlertService.sendAlert(eventWithLocation);

        verify(fcmNotificationPort).sendBatch(
                argThat(tokens -> tokens.size() == 2 && tokens.containsAll(List.of("token-001", "token-002"))),
                argThat(payload -> payload.containsKey("latitude") && payload.containsKey("longitude")));
    }

    @Test
    void sendAlert_fcmFailure_shouldTriggerSmsFallbackPlaceholder() {
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(true);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001", "token-002"));
        doThrow(new RuntimeException("FCM unavailable")).when(fcmNotificationPort).sendBatch(anyList(), any());

        familyAlertService.sendAlert(EmergencyTestFactory.makeEmergencySessionOpenedEvent());

        verify(smsFallbackPort).sendFallback(eq(USER_ID), eq(SESSION_ID), any());
        verify(familyAlertLogRepository).save(any());
    }

    @Test
    void sendAlert_fcmAndSmsFailure_shouldThrowAndNotRecordDeliveredLog() {
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(true);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001"));
        doThrow(new RuntimeException("FCM unavailable"))
                .when(fcmNotificationPort).sendBatch(anyList(), any());
        doThrow(new IllegalStateException("SMS unavailable"))
                .when(smsFallbackPort).sendFallback(eq(USER_ID), eq(SESSION_ID), any());

        assertThatThrownBy(() -> familyAlertService.sendAlert(
                        EmergencyTestFactory.makeEmergencySessionOpenedEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMS unavailable");

        verify(familyAlertLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sendAlert_payload_shouldContainUc161SafetyFields() {
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001"));

        familyAlertService.sendAlert(EmergencyTestFactory.makeEmergencySessionOpenedEvent());

        verify(fcmNotificationPort).sendBatch(anyList(), argThat((Map<String, String> payload) ->
                SESSION_ID.toString().equals(payload.get("safetyEventId"))
                        && USER_ID.toString().equals(payload.get("userId"))
                        && payload.containsKey("detectedAt")));
    }
}
