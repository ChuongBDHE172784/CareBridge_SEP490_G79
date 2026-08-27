package com.carebridge.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.VaccinationReminderCommand;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.impl.VaccinationNotificationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationNotificationServiceTest {

    private static final UUID RECORD_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID BABY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final LocalDate SCHEDULED = LocalDate.of(2026, 8, 14);

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private VaccinationNotificationService service;

    private VaccinationReminderCommand command(int daysBefore) {
        return new VaccinationReminderCommand(RECORD_ID, BABY_ID, OWNER_ID, "Bean",
                "Vắc-xin 6 trong 1", (short) 1, SCHEDULED, daysBefore);
    }

    private void arrangePushEnabled() {
        when(preferenceRepository.isPushEnabled(OWNER_ID, NotificationType.REMINDER)).thenReturn(true);
    }

    private void arrangeDevice() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(OWNER_ID))
                .thenReturn(List.of(DeviceToken.builder().token("token-1").build()));
    }

    private void arrangeNoPriorMilestone() {
        when(notificationRecordRepository.findVaccinationReminderByRecordAndLead(eq(RECORD_ID), anyString()))
                .thenReturn(Optional.empty());
    }

    private void arrangeSaveEcho() {
        when(notificationRecordRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            NotificationRecord record = invocation.getArgument(0);
            if (record.getId() == null) record.setId(UUID.randomUUID());
            return record;
        });
    }

    private NotificationRecord captureSaved() {
        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void send_sevenDayLead_deliversAReminderTaggedForVaccination() {
        arrangePushEnabled();
        arrangeNoPriorMilestone();
        arrangeDevice();
        arrangeSaveEcho();
        when(fcmService.sendWithRetry(eq("token-1"), anyString(), anyString(), anyMap(), anyInt()))
                .thenReturn(FcmDeliveryResult.success("msg-1", 1));

        NotificationRecordResponse response = service.sendVaccinationReminder(command(7));

        assertThat(response).isNotNull();
        NotificationRecord saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(NotificationRecordStatus.SENT);
        // The type column is constrained to the seven canonical values, so vaccination
        // reminders ride REMINDER and are distinguished by reference_type.
        assertThat(saved.getType()).isEqualTo(NotificationType.REMINDER);
        assertThat(saved.getReferenceType()).isEqualTo("VACCINATION");
        assertThat(saved.getReferenceId()).isEqualTo(RECORD_ID);
        assertThat(saved.getMetadata())
                .containsEntry("vaccinationRecordId", RECORD_ID.toString())
                .containsEntry("daysBefore", "7")
                .containsEntry("babyId", BABY_ID.toString());
        assertThat(saved.getTitle()).isEqualTo("Sắp đến lịch tiêm của bé");
        assertThat(saved.getBody()).contains("Bean", "Vắc-xin 6 trong 1", "14/08/2026", "còn 7 ngày");
        verify(auditService).log(eq(AuditAction.NOTIFICATION_SENT), eq(OWNER_ID), anyString(), anyString(), anyString());
    }

    @Test
    void send_dayOfLead_usesTheDayOfWording() {
        arrangePushEnabled();
        arrangeNoPriorMilestone();
        arrangeDevice();
        arrangeSaveEcho();
        when(fcmService.sendWithRetry(anyString(), anyString(), anyString(), anyMap(), anyInt()))
                .thenReturn(FcmDeliveryResult.success("msg-1", 1));

        service.sendVaccinationReminder(command(0));

        NotificationRecord saved = captureSaved();
        assertThat(saved.getTitle()).isEqualTo("Hôm nay là ngày tiêm của bé");
        assertThat(saved.getBody()).contains("hôm nay 14/08/2026");
    }

    @Test
    void send_milestoneAlreadyDelivered_isNotSentTwice() {
        arrangePushEnabled();
        NotificationRecord delivered = NotificationRecord.builder()
                .id(UUID.randomUUID()).userId(OWNER_ID).type(NotificationType.REMINDER)
                .title("t").body("b").referenceId(RECORD_ID).referenceType("VACCINATION")
                .status(NotificationRecordStatus.SENT).build();
        when(notificationRecordRepository.findVaccinationReminderByRecordAndLead(RECORD_ID, "7"))
                .thenReturn(Optional.of(delivered));

        assertThat(service.sendVaccinationReminder(command(7))).isNull();
        verify(fcmService, never()).sendWithRetry(anyString(), anyString(), anyString(), anyMap(), anyInt());
        verify(notificationRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void send_previousAttemptFailed_isRetriedOnTheSameRecord() {
        arrangePushEnabled();
        NotificationRecord failed = NotificationRecord.builder()
                .id(UUID.randomUUID()).userId(OWNER_ID).type(NotificationType.REMINDER)
                .title("t").body("b").referenceId(RECORD_ID).referenceType("VACCINATION")
                .status(NotificationRecordStatus.FAILED).build();
        when(notificationRecordRepository.findVaccinationReminderByRecordAndLead(RECORD_ID, "1"))
                .thenReturn(Optional.of(failed));
        arrangeDevice();
        arrangeSaveEcho();
        when(fcmService.sendWithRetry(anyString(), anyString(), anyString(), anyMap(), anyInt()))
                .thenReturn(FcmDeliveryResult.success("msg-2", 2));

        assertThat(service.sendVaccinationReminder(command(1))).isNotNull();
        assertThat(captureSaved().getId()).isEqualTo(failed.getId());
    }

    @Test
    void send_motherDisabledReminderPush_sendsNothing() {
        when(preferenceRepository.isPushEnabled(OWNER_ID, NotificationType.REMINDER)).thenReturn(false);

        assertThat(service.sendVaccinationReminder(command(3))).isNull();
        verify(notificationRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void send_noRegisteredDevice_recordsTheFailedDelivery() {
        arrangePushEnabled();
        arrangeNoPriorMilestone();
        when(deviceTokenRepository.findByUserIdAndActiveTrue(OWNER_ID)).thenReturn(List.of());
        arrangeSaveEcho();

        assertThat(service.sendVaccinationReminder(command(3))).isNotNull();
        assertThat(captureSaved().getStatus()).isEqualTo(NotificationRecordStatus.FAILED);
        verify(auditService).log(eq(AuditAction.NOTIFICATION_FAILED), eq(OWNER_ID), anyString(), anyString(), anyString());
    }
}
