package com.carebridge.backend.emergency;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.service.AlertRecipientEndpoint;
import com.carebridge.backend.emergency.service.EmergencyAlertAttemptService;
import com.carebridge.backend.emergency.service.EmergencyAlertClaim;
import com.carebridge.backend.emergency.service.EmergencyAlertProviderFence;
import com.carebridge.backend.emergency.service.EmergencyAlertDeliveryPersistenceService;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.FencedAlertDelivery;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.PreparedAlertDelivery;
import com.carebridge.backend.emergency.service.SmsFallbackPort;
import com.carebridge.backend.emergency.service.impl.FamilyAlertService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyAlertServiceTest {
    @Mock private FamilyMemberPort familyMemberPort;
    @Mock private FcmNotificationPort fcmNotificationPort;
    @Mock private LocationConsentPort locationConsentPort;
    @Mock private SmsFallbackPort smsFallbackPort;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EmergencyAlertAttemptService alertAttemptService;
    @Mock private EmergencyAlertProviderFence providerFence;
    @Mock private EmergencyAlertDeliveryPersistenceService deliveryPersistenceService;
    @Mock private AuditService auditService;
    @InjectMocks private FamilyAlertService familyAlertService;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final EmergencyAlertClaim CLAIM = new EmergencyAlertClaim(
            SESSION_ID, 1, UUID.fromString("00000000-0000-0000-0000-000000000020"),
            Instant.now().plusSeconds(120));

    @BeforeEach
    void defaults() {
        lenient().when(alertAttemptService.claim(SESSION_ID)).thenReturn(Optional.of(CLAIM));
        lenient().when(alertAttemptService.renew(CLAIM)).thenReturn(true);
        lenient().when(alertAttemptService.complete(
                eq(CLAIM), anyString(), anyInt(), anyInt(), anyBoolean())).thenReturn(true);
        lenient().when(deliveryPersistenceService.prepare(
                        any(), any(), nullable(UUID.class), eq(CLAIM)))
                .thenAnswer(invocation -> new PreparedAlertDelivery(
                        UUID.randomUUID(), UUID.randomUUID(), false, 0));
        lenient().when(deliveryPersistenceService.complete(
                any(PreparedAlertDelivery.class), eq(CLAIM), any())).thenReturn(true);
        lenient().when(providerFence.execute(eq(CLAIM), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<FencedAlertDelivery> operation = invocation.getArgument(1);
            return Optional.of(operation.get());
        });
        lenient().when(fcmNotificationPort.send(anyString(), any()))
                .thenReturn(FcmDeliveryResult.success("message-id", 1));
    }

    @Test
    void activeLeaseOrCompletedAttemptSkipsReplay() {
        when(alertAttemptService.claim(SESSION_ID)).thenReturn(Optional.empty());
        familyAlertService.sendAlert(event());
        verifyNoInteractions(familyMemberPort, fcmNotificationPort, deliveryPersistenceService);
    }

    @Test
    void noRecipientsCompletesIntentionalNoOpWithoutFallback() {
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(List.of());

        familyAlertService.sendAlert(event());

        verify(alertAttemptService).complete(CLAIM, "NO_RECIPIENTS", 0, 0, false);
        verifyNoInteractions(fcmNotificationPort, smsFallbackPort, deliveryPersistenceService);
    }

    @Test
    void noConsentExcludesLocation() {
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        familyAlertService.sendAlert(eventWithLocation());
        verify(fcmNotificationPort).send(eq("token-1"),
                argThat(payload -> !payload.containsKey("latitude") && !payload.containsKey("longitude")));
    }

    @Test
    void consentIncludesLocation() {
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(true);
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        familyAlertService.sendAlert(eventWithLocation());
        verify(fcmNotificationPort).send(eq("token-1"),
                argThat(payload -> payload.containsKey("latitude") && payload.containsKey("longitude")));
    }

    @Test
    void payloadRetainsLifecycleSafetyEventIdentity() {
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));

        familyAlertService.sendAlert(event());

        verify(fcmNotificationPort).send(eq("token-1"), argThat(payload ->
                SESSION_ID.toString().equals(payload.get("sessionId"))
                        && SESSION_ID.toString().equals(payload.get("safetyEventId"))
                        && USER_ID.toString().equals(payload.get("userId"))
                        && payload.containsKey("detectedAt")));
    }

    @Test
    void payloadCarriesDeterministicDeliveryActionIdentity() {
        UUID deliveryId = UUID.fromString("00000000-0000-0000-0000-000000000077");
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        when(deliveryPersistenceService.prepare(
                any(), any(), nullable(UUID.class), eq(CLAIM)))
                .thenReturn(new PreparedAlertDelivery(
                        deliveryId, UUID.randomUUID(), false, 0));

        familyAlertService.sendAlert(event());

        verify(fcmNotificationPort).send(eq("token-1"), argThat(payload ->
                deliveryId.toString().equals(payload.get("deliveryActionId"))));
    }

    @Test
    void allFailuresRetryReusesEvidenceAndTriggersFallback() {
        when(alertAttemptService.claim(SESSION_ID))
                .thenReturn(Optional.of(CLAIM), Optional.of(CLAIM));
        List<AlertRecipientEndpoint> recipients = recipients("token-1", "token-2");
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients);
        when(fcmNotificationPort.send(anyString(), any())).thenReturn(FcmDeliveryResult.failed("DOWN", 1));

        familyAlertService.sendAlert(event());
        familyAlertService.sendAlert(event());

        verify(fcmNotificationPort, times(4)).send(anyString(), any());
        verify(deliveryPersistenceService, times(4))
                .prepare(any(), any(), nullable(UUID.class), eq(CLAIM));
        verify(deliveryPersistenceService, times(4))
                .complete(any(PreparedAlertDelivery.class), eq(CLAIM), any());
        verify(alertAttemptService, times(2)).complete(CLAIM, "FAILED", 0, 2, false);
        verify(smsFallbackPort, times(2)).sendFallback(eq(USER_ID), eq(SESSION_ID), any());
    }

    @Test
    void partialRetryDoesNotResendSuccessfulDevice() {
        when(alertAttemptService.claim(SESSION_ID))
                .thenReturn(Optional.of(CLAIM), Optional.of(CLAIM));
        List<AlertRecipientEndpoint> recipients = recipients("token-1", "token-2");
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients);
        AtomicInteger tokenOnePrepare = new AtomicInteger();
        when(deliveryPersistenceService.prepare(any(), any(), nullable(UUID.class), eq(CLAIM)))
                .thenAnswer(invocation -> {
                    AlertRecipientEndpoint recipient = invocation.getArgument(1);
                    boolean priorSuccess = "token-1".equals(recipient.token())
                            && tokenOnePrepare.getAndIncrement() > 0;
                    return new PreparedAlertDelivery(UUID.randomUUID(), UUID.randomUUID(), priorSuccess, 1);
                });
        when(fcmNotificationPort.send(eq("token-1"), any()))
                .thenReturn(FcmDeliveryResult.success("m1", 1));
        when(fcmNotificationPort.send(eq("token-2"), any()))
                .thenReturn(FcmDeliveryResult.failed("DOWN", 1), FcmDeliveryResult.success("m2", 1));

        familyAlertService.sendAlert(event());
        familyAlertService.sendAlert(event());

        verify(fcmNotificationPort, times(1)).send(eq("token-1"), any());
        verify(fcmNotificationPort, times(2)).send(eq("token-2"), any());
        verify(alertAttemptService).complete(CLAIM, "PARTIAL", 1, 1, false);
        verify(alertAttemptService).complete(CLAIM, "SENT", 2, 0, false);
    }

    @Test
    void auditFailurePreventsExternalDeliveryAndLeavesAttemptForLeaseRetry() {
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        doThrow(new RuntimeException("audit unavailable")).when(auditService)
                .log(any(), any(), anyString(), anyString(), anyMap());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> familyAlertService.sendAlert(event()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("audit unavailable");

        verify(deliveryPersistenceService).prepare(any(), any(), any(), eq(CLAIM));
        verifyNoInteractions(fcmNotificationPort);
        verify(deliveryPersistenceService, never()).complete(any(), any(), any());
        verify(alertAttemptService, never()).complete(any(), anyString(), anyInt(), anyInt(), anyBoolean());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void crashAfterClaimLeavesLeaseToBlockImmediateReplay() {
        when(alertAttemptService.claim(SESSION_ID))
                .thenReturn(Optional.of(CLAIM), Optional.empty());
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID))
                .thenThrow(new RuntimeException("crash"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> familyAlertService.sendAlert(event()))
                .isInstanceOf(RuntimeException.class);
        familyAlertService.sendAlert(event());

        verify(familyMemberPort, times(1)).getFamilyAlertRecipients(USER_ID);
        verifyNoInteractions(fcmNotificationPort, deliveryPersistenceService);
    }

    @Test
    void staleLeaseCanBeReclaimedAndProcessed() {
        when(alertAttemptService.claim(SESSION_ID))
                .thenReturn(Optional.empty(), Optional.of(CLAIM));
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));

        familyAlertService.sendAlert(event());
        familyAlertService.sendAlert(event());

        verify(fcmNotificationPort, times(1)).send(eq("token-1"), any());
        verify(alertAttemptService).complete(CLAIM, "SENT", 1, 0, false);
    }

    @Test
    void noRecipientsOutcomeCanBeReclaimedWhenRecipientAppears() {
        when(alertAttemptService.claim(SESSION_ID))
                .thenReturn(Optional.of(CLAIM), Optional.of(CLAIM));
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID))
                .thenReturn(List.of(), recipients("token-1"));

        familyAlertService.sendAlert(event());
        familyAlertService.sendAlert(event());

        verify(alertAttemptService).complete(CLAIM, "NO_RECIPIENTS", 0, 0, false);
        verify(fcmNotificationPort).send(eq("token-1"), any());
        verify(alertAttemptService).complete(CLAIM, "SENT", 1, 0, false);
    }

    @Test
    void resolvedOrStaleClaimIsRevalidatedBeforeExternalSend() {
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        when(alertAttemptService.renew(CLAIM)).thenReturn(false);

        familyAlertService.sendAlert(event());

        verifyNoInteractions(fcmNotificationPort);
        verify(deliveryPersistenceService, never()).complete(any(), any(), any());
        verify(alertAttemptService, never()).complete(any(), anyString(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void staleFenceCannotRecordDeliveryOrCompleteAttempt() {
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        when(deliveryPersistenceService.complete(
                any(PreparedAlertDelivery.class), eq(CLAIM), any())).thenReturn(false);

        familyAlertService.sendAlert(event());

        verify(fcmNotificationPort).send(eq("token-1"), any());
        verify(alertAttemptService, never()).complete(any(), anyString(), anyInt(), anyInt(), anyBoolean());
        verifyNoInteractions(eventPublisher, smsFallbackPort);
    }

    @Test
    void resolutionBetweenIntentAndProviderFenceSuppressesExternalSend() {
        when(familyMemberPort.getFamilyAlertRecipients(USER_ID)).thenReturn(recipients("token-1"));
        when(providerFence.execute(eq(CLAIM), any())).thenReturn(Optional.empty());

        familyAlertService.sendAlert(event());

        verifyNoInteractions(fcmNotificationPort);
        verify(deliveryPersistenceService, never()).complete(any(), any(), any());
        verify(alertAttemptService, never()).complete(any(), anyString(), anyInt(), anyInt(), anyBoolean());
    }

    private EmergencySessionOpened event() {
        return EmergencyTestFactory.makeEmergencySessionOpenedEvent();
    }

    private EmergencySessionOpened eventWithLocation() {
        return new EmergencySessionOpened(UUID.randomUUID(), SESSION_ID, USER_ID, "MANUAL",
                new BigDecimal("10.123"), new BigDecimal("106.456"), Instant.now());
    }

    private static List<AlertRecipientEndpoint> recipients(String... tokens) {
        return java.util.stream.IntStream.range(0, tokens.length)
                .mapToObj(index -> new AlertRecipientEndpoint(
                        UUID.nameUUIDFromBytes(("recipient-" + index).getBytes()),
                        UUID.nameUUIDFromBytes(("device-" + index).getBytes()), tokens[index]))
                .toList();
    }
}
