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

    public void notifyAssignedForReview(
            UUID expertUserId,
            UUID targetId,
            String targetType,
            String targetTitle) {
        if (expertUserId == null) {
            return;
        }
        Instant now = Instant.now();
        notificationRecordRepository.save(NotificationRecord.builder()
                .userId(expertUserId)
                .type(NotificationType.CONTENT_REVIEW)
                .title("Nội dung mới cần thẩm định")
                .body("Bạn được phân công thẩm định " + (targetType.equalsIgnoreCase("CHECKLIST") ? "checklist" : "nội dung") + ": \"" + targetTitle + "\"")
                .referenceId(targetId)
                .referenceType(targetType)
                .status(NotificationRecordStatus.SENT)
                .channel("IN_APP")
                .sentAt(now)
                .metadata(Map.of("route", "/expert/content-approval"))
                .build());
    }

    /**
     * Notifies the author of reported community content that a moderator has acted on it
     * (UC-102 report resolution: REQUEST_REVISION / WARN).
     *
     * <p>Reuses {@link NotificationType#CONTENT_REVIEW} and the existing
     * {@code notification_records} table — no new type, column or table is introduced.
     *
     * @param title    user-facing heading ("Yêu cầu sửa nội dung" / "Cảnh báo từ kiểm duyệt viên")
     * @param note     the moderator's handling note; shown verbatim to the author
     * @param outcome  recorded in metadata so the client can distinguish the two cases
     */
    public void notifyModerationOutcome(
            UUID recipientUserId,
            UUID targetId,
            String targetType,
            String targetLabel,
            String title,
            String note,
            String outcome) {
        if (recipientUserId == null) {
            return;
        }
        String body = "\"" + targetLabel + "\"";
        if (note != null && !note.isBlank()) {
            body = body + " — " + note.trim();
        }
        notificationRecordRepository.save(NotificationRecord.builder()
                .userId(recipientUserId)
                .type(NotificationType.CONTENT_REVIEW)
                .title(title)
                .body(body)
                .referenceId(targetId)
                .referenceType(targetType)
                .status(NotificationRecordStatus.SENT)
                .channel("IN_APP")
                .sentAt(Instant.now())
                .metadata(Map.of("outcome", outcome))
                .build());
    }
}

