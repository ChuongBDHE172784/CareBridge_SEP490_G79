package com.carebridge.backend.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.ContentReviewNotificationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentReviewNotificationServiceTest {

    @Mock
    private NotificationRecordRepository notificationRecordRepository;

    @InjectMocks
    private ContentReviewNotificationService service;

    @Test
    void notifyReturned_persistsOneActionableInAppNotification() {
        UUID recipientId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();

        service.notifyReturned(recipientId, contentId, "ARTICLE", "Dinh dưỡng thai kỳ",
                "Bổ sung nguồn", "/content/" + contentId + "/edit");

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).save(captor.capture());
        NotificationRecord record = captor.getValue();
        assertEquals(recipientId, record.getUserId());
        assertEquals(NotificationType.CONTENT_REVIEW, record.getType());
        assertEquals(NotificationRecordStatus.SENT, record.getStatus());
        assertEquals("IN_APP", record.getChannel());
        assertEquals(contentId, record.getReferenceId());
        assertEquals("/content/" + contentId + "/edit", record.getMetadata().get("route"));
    }
}
