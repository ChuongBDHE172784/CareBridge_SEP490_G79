package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.RegisterDeviceTokenRequest;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.NotificationService;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Override
    @Transactional
    public void registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request) {
        deviceTokenRepository.findByUserIdAndToken(userId, request.token())
                .ifPresentOrElse(
                        existing -> {
                            existing.setActive(true);
                            existing.setPlatform(request.platform());
                            deviceTokenRepository.save(existing);
                        },
                        () -> deviceTokenRepository.save(DeviceToken.builder()
                                .userId(userId)
                                .token(request.token())
                                .platform(request.platform())
                                .active(true)
                                .build())
                );
        log.debug("Device token registered for userId={}, platform={}", userId, request.platform());
    }

    @Override
    @Transactional
    public void deregisterDeviceToken(UUID userId, String token) {
        deviceTokenRepository.deactivateByToken(token, Instant.now());
    }

    @Override
    @Transactional
    public NotificationRecordResponse send(SendNotificationRequest request) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(request.recipientUserId());

        NotificationRecord record = NotificationRecord.builder()
                .userId(request.recipientUserId())
                .type(request.type())
                .title(request.title())
                .body(request.body())
                .referenceId(request.referenceId())
                .referenceType(request.referenceType())
                .status(NotificationRecordStatus.SENT)
                .attemptCount(1)
                .build();

        if (tokens.isEmpty()) {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now());
            NotificationRecord saved = notificationRecordRepository.save(record);
            auditService.log(AuditAction.NOTIFICATION_FAILED, request.recipientUserId(),
                    "notification", saved.getId().toString(), "No active device tokens");
            return toResponse(saved);
        }

        List<String> fcmTokens = tokens.stream().map(DeviceToken::getToken).toList();
        String fcmMessageId = null;
        try {
            if (fcmTokens.size() == 1) {
                fcmMessageId = fcmService.sendToToken(fcmTokens.get(0), request.title(), request.body());
            } else {
                fcmService.sendToTokens(fcmTokens, request.title(), request.body());
            }
            record.setStatus(NotificationRecordStatus.SENT);
            record.setSentAt(Instant.now());
            record.setFcmMessageId(fcmMessageId);
        } catch (Exception e) {
            log.error("FCM delivery failed for userId={}: {}", request.recipientUserId(), e.getMessage());
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now());
        }

        NotificationRecord saved = notificationRecordRepository.save(record);

        AuditAction auditAction = (saved.getStatus() == NotificationRecordStatus.FAILED)
                ? AuditAction.NOTIFICATION_FAILED
                : AuditAction.NOTIFICATION_SENT;
        auditService.log(auditAction, request.recipientUserId(), "notification", saved.getId().toString(),
                request.type().name());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationRecordResponse> getMyNotifications(UUID userId, String type, Pageable pageable,
            Principal principal) {
        Page<NotificationRecord> page;
        if (type != null && !type.isBlank()) {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
            page = notificationRecordRepository.findByUserIdAndType(userId, notificationType, pageable);
        } else {
            page = notificationRecordRepository.findByUserId(userId, pageable);
        }
        return page.map(this::toResponse);
    }

    private NotificationRecordResponse toResponse(NotificationRecord r) {
        return new NotificationRecordResponse(
                r.getId(),
                r.getUserId(),
                r.getType().name(),
                r.getTitle(),
                r.getBody(),
                r.getReferenceId(),
                r.getReferenceType(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getSentAt()
        );
    }
}
