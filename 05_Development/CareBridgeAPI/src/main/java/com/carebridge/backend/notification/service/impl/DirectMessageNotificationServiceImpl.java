package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.repository.NotificationPreferenceRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.IDirectMessageNotificationService;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// ADR-MEDI-004 mục 2-5, mirrors CommunityReplyNotificationService's insert/deliver/audit shape.
@Service
public class DirectMessageNotificationServiceImpl implements IDirectMessageNotificationService {

    // Reused as-is from CommunityReplyNotificationService — not a new value invented for this feature.
    private static final int MAX_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRecordWriter notificationRecordWriter;
    private final FcmService fcmService;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public DirectMessageNotificationServiceImpl(
            UserRepository userRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationRecordRepository notificationRecordRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationRecordWriter notificationRecordWriter,
            FcmService fcmService,
            AuditService auditService) {
        this(userRepository, deviceTokenRepository, notificationRecordRepository, preferenceRepository, notificationRecordWriter,
                fcmService, auditService, Clock.systemDefaultZone());
    }

    /** Test constructor — allows injecting a fixed Clock for deterministic time calculations. */
    public DirectMessageNotificationServiceImpl(
            UserRepository userRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationRecordRepository notificationRecordRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationRecordWriter notificationRecordWriter,
            FcmService fcmService,
            AuditService auditService,
            Clock clock) {
        this.userRepository = userRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRecordRepository = notificationRecordRepository;
        this.preferenceRepository = preferenceRepository;
        this.notificationRecordWriter = notificationRecordWriter;
        this.fcmService = fcmService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public void notifyNewMessage(UUID recipientUserId, UUID senderUserId, UUID conversationId, UUID messageId) {
        if (!preferenceRepository.isPushEnabled(recipientUserId, NotificationType.MESSAGE)) {
            return;
        }
        String senderDisplayName = userRepository.findById(senderUserId)
                .map(u -> u.getName())
                .orElse("một người dùng");
        String title = "Tin nhắn mới";
        // C4 — never includes the message body itself, only the sender's name.
        String body = "Bạn có tin nhắn mới từ " + senderDisplayName;

        NotificationRecord candidate = NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(recipientUserId)
                .type(NotificationType.MESSAGE)
                .title(title)
                .body(body)
                .referenceId(messageId)
                .referenceType("DIRECT_MESSAGE")
                .status(NotificationRecordStatus.PENDING)
                .attemptCount(0)
                .createdAt(Instant.now(clock))
                .metadata(Map.of(
                        "conversationId", conversationId.toString(),
                        "senderUserId", senderUserId.toString(),
                        "eventType", "MESSAGE_SENT"))
                .build();

        // C10 — DB-enforced idempotency, independent of clientMessageId's application-level guard.
        boolean isNew = notificationRecordWriter.insertIfAbsent(candidate);
        if (!isNew) {
            return; // duplicate listener run for the same (recipient, messageId) — do not push twice
        }

        deliver(candidate);
    }

    public void retryPendingNotifications() {
        for (UUID id : notificationRecordWriter.findPendingIds(50)) {
            notificationRecordRepository.findById(id).ifPresent(this::deliver);
        }
    }

    private void deliver(NotificationRecord candidate) {
        if (!notificationRecordWriter.claim(candidate.getId())) {
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(candidate.getUserId());
        if (tokens.isEmpty()) {
            candidate.setStatus(NotificationRecordStatus.FAILED);
            candidate.setAttemptCount(0); // no token to call FCM with
            candidate.setFailedAt(Instant.now(clock));
        } else {
            try {
                FcmDeliveryResult delivery = fcmService.sendWithRetry(
                        tokens.getFirst().getToken(), candidate.getTitle(), candidate.getBody(), MAX_ATTEMPTS);
                applyDelivery(candidate, delivery);
            } catch (Exception e) {
                // C11 — sentinel 0: no FcmDeliveryResult ever completed, distinct from a real
                // delivery.attempts() count. Never rethrown — nothing above this to roll back
                // (AFTER_COMMIT), but the failure must still leave evidence, not vanish silently.
                candidate.setStatus(NotificationRecordStatus.FAILED);
                candidate.setAttemptCount(0);
                candidate.setFailedAt(Instant.now(clock));
            }
        }
        notificationRecordWriter.complete(candidate);
        audit(candidate);
    }

    private void applyDelivery(NotificationRecord record, FcmDeliveryResult delivery) {
        record.setAttemptCount(delivery.attempts());
        if (delivery.success()) {
            record.setStatus(NotificationRecordStatus.SENT);
            record.setFcmMessageId(delivery.messageId());
            record.setSentAt(Instant.now(clock));
        } else {
            record.setStatus(NotificationRecordStatus.FAILED);
            record.setFailedAt(Instant.now(clock));
        }
    }

    private void audit(NotificationRecord record) {
        AuditAction action = record.getStatus() == NotificationRecordStatus.FAILED
                ? AuditAction.NOTIFICATION_FAILED
                : AuditAction.NOTIFICATION_SENT;
        try {
            auditService.log(action, record.getUserId(), "NotificationRecord", record.getId().toString(), record.getType().name());
        } catch (RuntimeException ignored) {
            // Delivery state was committed in REQUIRES_NEW before audit. Audit failure must never
            // erase the durable outbox/notification record or cause the push to be repeated.
        }
    }
}
