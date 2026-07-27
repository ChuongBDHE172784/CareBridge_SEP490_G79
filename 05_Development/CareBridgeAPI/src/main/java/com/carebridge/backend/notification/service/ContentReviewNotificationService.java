package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentReviewNotificationService {

    private final NotificationRecordRepository notificationRecordRepository;

    public void notifyReturned(
            UUID recipientUserId,
            UUID targetId,
            String targetType,
            String targetTitle,
            String reason,
            String route) {
        if (recipientUserId == null) {
            return;
        }
        Instant now = Instant.now();
        notificationRecordRepository.save(NotificationRecord.builder()
                .userId(recipientUserId)
                .type(NotificationType.CONTENT_REVIEW)
                .title("Nội dung cần chỉnh sửa")
                .body("\"" + targetTitle + "\" đã được trả về: " + reason)
                .referenceId(targetId)
                .referenceType(targetType)
                .status(NotificationRecordStatus.SENT)
                .channel("IN_APP")
                .sentAt(now)
                .metadata(Map.of("route", route))
                .build());
    }
}

