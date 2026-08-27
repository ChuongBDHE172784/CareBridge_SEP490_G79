package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.repository.EmergencyAlertDeliveryRepository;
import com.carebridge.backend.emergency.service.AlertRecipientEndpoint;
import com.carebridge.backend.emergency.service.EmergencyAlertDeliveryPersistenceService;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyAlertDeliveryPersistenceServiceTest {

    @Mock private EmergencyAlertDeliveryRepository deliveryRepository;
    @Mock private NotificationRecordRepository notificationRepository;
    @InjectMocks private EmergencyAlertDeliveryPersistenceService service;

    @Test
    void tokenlessRecipientNotificationIsImmediatelyVisibleInApp() {
        UUID notificationId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();
        UUID careGroupId = UUID.randomUUID();
        var event = EmergencyTestFactory.makeEmergencySessionOpenedEvent();
        var recipient = AlertRecipientEndpoint.inAppOnly(recipientUserId, careGroupId);
        when(notificationRepository.findByUserIdAndReferenceIdAndTypeAndReferenceTypeAndCareGroupId(
                any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(notificationRepository.saveAndFlush(any(NotificationRecord.class)))
                .thenAnswer(invocation -> {
                    NotificationRecord record = invocation.getArgument(0);
                    record.setId(notificationId);
                    return record;
                });
        when(notificationRepository.save(any(NotificationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID persistedId = service.persistInAppNotification(event, recipient, false);

        ArgumentCaptor<NotificationRecord> recordCaptor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRepository).save(recordCaptor.capture());
        NotificationRecord record = recordCaptor.getValue();
        assertThat(persistedId).isEqualTo(notificationId);
        assertThat(record.getUserId()).isEqualTo(recipientUserId);
        assertThat(record.getCareGroupId()).isEqualTo(careGroupId);
        assertThat(record.getStatus()).isEqualTo(NotificationRecordStatus.SENT);
        assertThat(record.getChannel()).isEqualTo("IN_APP");
        assertThat(record.getSentAt()).isNotNull();
    }
}
