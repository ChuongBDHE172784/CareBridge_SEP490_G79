package com.carebridge.backend.notification.service.impl;

import static com.carebridge.backend.notification.EpdsNotificationProps.FAMILY_1;
import static com.carebridge.backend.notification.EpdsNotificationProps.FAMILY_2;
import static com.carebridge.backend.notification.EpdsNotificationProps.GROUP_ID;
import static com.carebridge.backend.notification.EpdsNotificationProps.GROUP_ID_2;
import static com.carebridge.backend.notification.EpdsNotificationProps.MOTHER_ID;
import static com.carebridge.backend.notification.EpdsNotificationProps.allowEpds;
import static com.carebridge.backend.notification.EpdsNotificationProps.makeEvent;
import static com.carebridge.backend.notification.EpdsNotificationProps.makeFamilyMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.service.NotificationService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CB-EPDS-TEST-001 — TC-01, TC-02, TC-09..TC-12, TC-14a, TC-15a/b, TC-16, TC-17, TC-22, TC-23.
 */
@ExtendWith(MockitoExtension.class)
class EpdsFamilyNotificationServiceTest {

    @Mock private NotificationService notificationService;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;

    @InjectMocks private EpdsFamilyNotificationService service;

    private ArgumentCaptor<SendNotificationRequest> captor() {
        return ArgumentCaptor.forClass(SendNotificationRequest.class);
    }

    // ---------------------------------------------------------------- TC-01
    @Test
    void consentedFamilyMemberReceivesNotification() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        ArgumentCaptor<SendNotificationRequest> captor = captor();
        verify(notificationService, times(1)).send(captor.capture());
        SendNotificationRequest sent = captor.getValue();
        assertThat(sent.recipientUserId()).isEqualTo(FAMILY_1);
        assertThat(sent.type()).isEqualTo(NotificationType.EPDS_RESULT);
        assertThat(sent.referenceId()).isEqualTo(GROUP_ID);
        assertThat(sent.referenceType()).isEqualTo("CARE_GROUP");
        assertThat(sent.title()).isNotBlank();
        assertThat(sent.body()).contains("Điểm 8/30");
    }

    // ---------------------------------------------------------------- TC-02
    @Test
    void twoConsentedMembersEachReceiveExactlyOne() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1), makeFamilyMember(FAMILY_2)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_2, true, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        ArgumentCaptor<SendNotificationRequest> captor = captor();
        verify(notificationService, times(2)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(SendNotificationRequest::recipientUserId)
                .containsExactlyInAnyOrder(FAMILY_1, FAMILY_2);
    }

    // ---------------------------------------------------------------- TC-09
    @Test
    void memberWithoutEpdsChildFlagReceivesNothing() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, false);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        verify(notificationService, never()).send(any());
    }

    // ---------------------------------------------------------------- TC-10
    @Test
    void revokedParentFlagDisablesChildFlag() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, false, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        verify(notificationService, never()).send(any());
    }

    // ---------------------------------------------------------------- TC-11
    @Test
    void mixedGroupNotifiesOnlyTheConsentedMember() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1), makeFamilyMember(FAMILY_2)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_2, true, false);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        ArgumentCaptor<SendNotificationRequest> captor = captor();
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getValue().recipientUserId()).isEqualTo(FAMILY_1);
    }

    // ---------------------------------------------------------------- TC-12
    @Test
    void motherNeverReceivesNotificationAboutHerOwnScreening() {
        // Defensive: even if a row bearing the mother's id leaks through the query.
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(MOTHER_ID), makeFamilyMember(FAMILY_1)));
        allowEpds(authorizationPolicy, GROUP_ID, MOTHER_ID, true, true);
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        ArgumentCaptor<SendNotificationRequest> captor = captor();
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(SendNotificationRequest::recipientUserId)
                .doesNotContain(MOTHER_ID);
    }

    // ---------------------------------------------------------------- TC-14a
    @Test
    void dispatchGoesThroughNotificationServiceSoAuditIsPreserved() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        // NotificationService.send owns the NOTIFICATION_SENT/FAILED audit; bypassing it
        // by writing NotificationRecord directly would silently drop the audit trail.
        verify(notificationService).send(any(SendNotificationRequest.class));
    }

    // ---------------------------------------------------------------- TC-15a
    @Test
    void listenerIsAsyncAndAfterCommit() throws NoSuchMethodException {
        Method handler = EpdsFamilyNotificationService.class
                .getMethod("onEpdsScreeningCompleted",
                        com.carebridge.backend.health.event.EpdsScreeningCompleted.class);

        assertThat(handler.isAnnotationPresent(Async.class)).isTrue();
        TransactionalEventListener listener = handler.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    // ---------------------------------------------------------------- TC-15b
    @Test
    void sendFailureIsSwallowedSoTheEpdsWriteIsUnaffected() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);
        when(notificationService.send(any())).thenThrow(new RuntimeException("FCM down"));

        assertThatCode(() -> service.onEpdsScreeningCompleted(makeEvent(8, 0)))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------- TC-17
    @Test
    void oneRecipientFailureDoesNotDropTheOthers() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(makeFamilyMember(FAMILY_1), makeFamilyMember(FAMILY_2)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_2, true, true);
        when(notificationService.send(any()))
                .thenThrow(new RuntimeException("FCM down"))
                .thenReturn(null);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        // Two invocations prove the catch sits inside the loop, not around it.
        verify(notificationService, times(2)).send(any());
    }

    // ---------------------------------------------------------------- TC-22
    @Test
    void memberInTwoOwnedGroupsReceivesExactlyOneNotification() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(
                        makeFamilyMember(FAMILY_1, GROUP_ID),
                        makeFamilyMember(FAMILY_1, GROUP_ID_2)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);
        allowEpds(authorizationPolicy, GROUP_ID_2, FAMILY_1, true, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        ArgumentCaptor<SendNotificationRequest> captor = captor();
        verify(notificationService, times(1)).send(captor.capture());
        // First group in care_group_id ASC order wins, so referenceId is deterministic.
        assertThat(captor.getValue().referenceId()).isEqualTo(GROUP_ID);
    }

    // ---------------------------------------------------------------- TC-23
    @Test
    void consentIsEvaluatedBeforeDeduplication() {
        // FAMILY_1 is in two groups: NOT consented in the first, consented in the second.
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of(
                        makeFamilyMember(FAMILY_1, GROUP_ID),
                        makeFamilyMember(FAMILY_1, GROUP_ID_2)));
        allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, false);
        allowEpds(authorizationPolicy, GROUP_ID_2, FAMILY_1, true, true);

        service.onEpdsScreeningCompleted(makeEvent(8, 0));

        ArgumentCaptor<SendNotificationRequest> captor = captor();
        // Zero invocations here would mean dedup ran first and collapsed onto the
        // non-consented row, suppressing a notification the mother did authorise.
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getValue().referenceId()).isEqualTo(GROUP_ID_2);
    }

    // ---------------------------------------------------------------- TC-24
    @Test
    void noRecipientsResolvesQuietlyWithoutSending() {
        when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                .thenReturn(List.of());

        assertThatCode(() -> service.onEpdsScreeningCompleted(makeEvent(8, 0)))
                .doesNotThrowAnyException();
        verify(notificationService, never()).send(any());
    }

    // ---------------------------------------------------------------- TC-16
    @Test
    void logsCarryCountsAndIdsButNeitherScoreNorQuestion10() {
        Logger serviceLogger = (Logger) LoggerFactory.getLogger(EpdsFamilyNotificationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        serviceLogger.addAppender(appender);

        try {
            when(memberRepository.findAcceptedFamilyMembersForEpdsAlerts(MOTHER_ID))
                    .thenReturn(List.of(makeFamilyMember(FAMILY_1), makeFamilyMember(FAMILY_2)));
            allowEpds(authorizationPolicy, GROUP_ID, FAMILY_1, true, true);
            allowEpds(authorizationPolicy, GROUP_ID, FAMILY_2, true, true);

            // Distinctive values so a leak is unambiguous rather than coincidental.
            service.onEpdsScreeningCompleted(makeEvent(27, 3));

            String logged = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (a, b) -> a + "\n" + b);

            // Guard against a vacuous pass on empty log output (AP-01).
            assertThat(logged).isNotBlank();
            assertThat(logged).contains("recipients=2");
            assertThat(logged).contains(GROUP_ID.toString());

            // NFR-EPDS-N-05: the mental-health score and the self-harm item must never be logged.
            assertThat(logged)
                    .as("total score must not appear in logs")
                    .doesNotContain("27");
            assertThat(logged)
                    .as("Question-10 score must not appear in logs")
                    .doesNotContain("question10")
                    .doesNotContain("q10");
        } finally {
            serviceLogger.detachAppender(appender);
            appender.stop();
        }
    }
}
