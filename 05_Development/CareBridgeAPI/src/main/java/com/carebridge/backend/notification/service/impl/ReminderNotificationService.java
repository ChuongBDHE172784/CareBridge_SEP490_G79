package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.ReminderNotificationCommand;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.IReminderNotificationService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderNotificationService implements IReminderNotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Override
    @Transactional
    public NotificationRecordResponse sendReminderNotification(UUID reminderId, UUID userId, String title, String body) {
        if (!preferenceRepository.isPushEnabled(userId, NotificationType.REMINDER)) {
            return null;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        NotificationRecord record = baseRecord(userId, NotificationType.REMINDER, title, body, reminderId, "REMINDER");
        if (tokens.isEmpty()) {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setAttemptCount(0);
            record.setFailedAt(Instant.now());
            return saveAndAudit(record);
        }

        FcmDeliveryResult delivery = fcmService.sendWithRetry(tokens.getFirst().getToken(), title, body, 3);
        applyDelivery(record, delivery);
        return saveAndAudit(record);
    }

    @Override
    @Transactional
    public NotificationRecordResponse sendAppointmentNotification(ReminderNotificationCommand command) {
        if (!preferenceRepository.isPushEnabled(command.userId(), NotificationType.REMINDER)) {
            return null;
        }
        NotificationRecord existing = notificationRecordRepository
                .findAppointmentMilestoneByJobId(command.jobId())
                .orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        String title = milestoneTitle(command.offsetMinutes());
        String body = milestoneBody(command);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("type", "REMINDER");
        metadata.put("milestoneJobId", command.jobId().toString());
        metadata.put("reminderId", command.reminderId().toString());
        metadata.put("occurrenceId", command.occurrenceId().toString());
        metadata.put("offsetMinutes", Integer.toString(command.offsetMinutes()));
        metadata.put("route", "/reminders/detail/" + command.reminderId());

        NotificationRecord record = baseRecord(
                command.userId(), NotificationType.REMINDER, title, body,
                command.reminderId(), "APPOINTMENT");
        record.setMetadata(metadata);
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(command.userId());
        if (tokens.isEmpty()) {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setAttemptCount(0);
            record.setFailedAt(Instant.now());
            return saveAndAudit(record);
        }

        int successfulDevices = 0;
        int totalAttempts = 0;
        String firstMessageId = null;
        for (DeviceToken token : tokens) {
            FcmDeliveryResult delivery = fcmService.sendWithRetry(
                    token.getToken(), title, body, metadata, 3);
            totalAttempts += delivery.attempts();
            if (delivery.success()) {
                successfulDevices++;
                if (firstMessageId == null) firstMessageId = delivery.messageId();
            }
        }
        metadata.put("deviceCount", Integer.toString(tokens.size()));
        metadata.put("successfulDeviceCount", Integer.toString(successfulDevices));
        record.setAttemptCount(totalAttempts);
        if (successfulDevices > 0) {
            record.setStatus(NotificationRecordStatus.SENT);
            record.setFcmMessageId(firstMessageId);
            record.setSentAt(Instant.now());
        } else {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now());
        }
        return saveAndAudit(record);
    }

    private String milestoneTitle(int offsetMinutes) {
        if (offsetMinutes > 0) return "Lịch hẹn chưa hoàn thành";
        if (offsetMinutes == 0) return "Đã đến giờ lịch hẹn";
        return "Sắp đến lịch hẹn";
    }

    private String milestoneBody(ReminderNotificationCommand command) {
        if (command.offsetMinutes() > 0) {
            return "Bạn chưa đánh dấu hoàn thành: " + command.appointmentTitle();
        }
        LocalDateTime local = LocalDateTime.ofInstant(
                command.occurrenceScheduledAt(), ZoneId.of(command.timeZone()));
        String time = local.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        return command.appointmentTitle() + " lúc " + time;
    }

    private NotificationRecord baseRecord(
            UUID userId, NotificationType type, String title, String body, UUID referenceId, String referenceType) {
        return NotificationRecord.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
    }

    private void applyDelivery(NotificationRecord record, FcmDeliveryResult delivery) {
        record.setAttemptCount(delivery.attempts());
        if (delivery.success()) {
            record.setStatus(NotificationRecordStatus.SENT);
            record.setFcmMessageId(delivery.messageId());
            record.setSentAt(Instant.now());
        } else {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now());
        }
    }

    private NotificationRecordResponse saveAndAudit(NotificationRecord record) {
        NotificationRecord saved = notificationRecordRepository.save(record);
        AuditAction action = saved.getStatus() == NotificationRecordStatus.FAILED
                ? AuditAction.NOTIFICATION_FAILED
                : AuditAction.NOTIFICATION_SENT;
        auditService.log(action, saved.getUserId(), "NotificationRecord", saved.getId().toString(),
                saved.getType().name());
        return toResponse(saved);
    }

    private NotificationRecordResponse toResponse(NotificationRecord record) {
        return new NotificationRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getType().name(),
                record.getTitle(),
                record.getBody(),
                record.getReferenceId(),
                record.getReferenceType(),
                record.getStatus().name(),
                record.getCreatedAt(),
                record.getSentAt(),
                record.isRead(),
                record.getReadAt(),
                record.getChannel(),
                record.getFcmMessageId(),
                record.getAttemptCount(),
                record.getFailedAt(),
                record.getUpdatedAt(),
                record.getMetadata()
        );
    }
}
