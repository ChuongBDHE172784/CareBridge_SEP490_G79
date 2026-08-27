package com.carebridge.backend.notification.service.impl;

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
import com.carebridge.backend.notification.service.IVaccinationNotificationService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vaccination reminders ride the existing REMINDER notification type — the
 * {@code notification_records_type_check} constraint admits no other value — and are
 * distinguished by {@code reference_type = 'VACCINATION'}.
 */
@Service
@RequiredArgsConstructor
public class VaccinationNotificationService implements IVaccinationNotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Override
    @Transactional
    public NotificationRecordResponse sendVaccinationReminder(VaccinationReminderCommand command) {
        if (!preferenceRepository.isPushEnabled(command.userId(), NotificationType.REMINDER)) {
            return null;
        }

        // (record, daysBefore) is the milestone identity: a delivered milestone is never
        // re-sent, so a job that runs more than once a day stays silent.
        NotificationRecord existing = notificationRecordRepository
                .findVaccinationReminderByRecordAndLead(
                        command.vaccinationRecordId(), Integer.toString(command.daysBefore()))
                .orElse(null);
        if (existing != null
                && (existing.getStatus() == NotificationRecordStatus.SENT
                || existing.getStatus() == NotificationRecordStatus.DELIVERED)) {
            return null;
        }

        String title = title(command);
        String body = body(command);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("type", "REMINDER");
        metadata.put("referenceType", "VACCINATION");
        metadata.put("referenceId", command.vaccinationRecordId().toString());
        metadata.put("vaccinationRecordId", command.vaccinationRecordId().toString());
        metadata.put("daysBefore", Integer.toString(command.daysBefore()));
        metadata.put("babyId", command.babyId().toString());
        metadata.put("vaccineName", command.vaccineName());
        metadata.put("scheduledDate", command.scheduledDate().toString());
        metadata.put("route", "/babies/" + command.babyId() + "/vaccinations");
        if (command.doseNumber() != null) {
            metadata.put("doseNumber", command.doseNumber().toString());
        }

        NotificationRecord record = existing != null ? existing : NotificationRecord.builder()
                .userId(command.userId())
                .type(NotificationType.REMINDER)
                .title(title)
                .body(body)
                .referenceId(command.vaccinationRecordId())
                .referenceType("VACCINATION")
                .build();
        record.setTitle(title);
        record.setBody(body);
        record.setReferenceId(command.vaccinationRecordId());
        record.setReferenceType("VACCINATION");
        record.setStatus(NotificationRecordStatus.PROCESSING);
        record.setFailedAt(null);
        record.setSentAt(null);
        record.setFcmMessageId(null);
        record.setAttemptCount(0);
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
            FcmDeliveryResult delivery = fcmService.sendWithRetry(token.getToken(), title, body, metadata, 3);
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

    private String title(VaccinationReminderCommand command) {
        return switch (command.daysBefore()) {
            case 0 -> "Hôm nay là ngày tiêm của bé";
            case 1 -> "Ngày mai bé đến lịch tiêm";
            default -> "Sắp đến lịch tiêm của bé";
        };
    }

    private String body(VaccinationReminderCommand command) {
        String baby = command.babyNickname() == null || command.babyNickname().isBlank()
                ? "Bé" : command.babyNickname();
        String dose = command.doseNumber() == null ? "" : " (mũi " + command.doseNumber() + ")";
        String when = switch (command.daysBefore()) {
            case 0 -> "hôm nay " + command.scheduledDate().format(DATE);
            case 1 -> "ngày mai " + command.scheduledDate().format(DATE);
            default -> "ngày " + command.scheduledDate().format(DATE)
                    + " (còn " + command.daysBefore() + " ngày)";
        };
        return baby + " có lịch tiêm " + command.vaccineName() + dose + " vào " + when + ".";
    }

    private NotificationRecordResponse saveAndAudit(NotificationRecord record) {
        NotificationRecord saved = notificationRecordRepository.saveAndFlush(record);
        AuditAction action = saved.getStatus() == NotificationRecordStatus.FAILED
                ? AuditAction.NOTIFICATION_FAILED
                : AuditAction.NOTIFICATION_SENT;
        auditService.log(action, saved.getUserId(), "NotificationRecord", saved.getId().toString(),
                "VACCINATION reminder");
        return new NotificationRecordResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getType().name(),
                saved.getTitle(),
                saved.getBody(),
                saved.getReferenceId(),
                saved.getReferenceType(),
                saved.getStatus().name(),
                saved.getCreatedAt(),
                saved.getSentAt(),
                saved.isRead(),
                saved.getReadAt(),
                saved.getChannel(),
                saved.getFcmMessageId(),
                saved.getAttemptCount(),
                saved.getFailedAt(),
                saved.getUpdatedAt(),
                saved.getMetadata());
    }
}
