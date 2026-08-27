package com.carebridge.backend.emergency.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.dto.request.ShareLocationRequest;
import com.carebridge.backend.emergency.dto.response.LocationShareResponse;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyLocationShareService {

    private static final String REFERENCE_TYPE = "LOCATION_SHARE";

    private final LocationConsentPort locationConsentPort;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final AuditService auditService;

    @Transactional
    public LocationShareResponse share(UUID motherId, ShareLocationRequest request) {
        if (!locationConsentPort.hasLocationConsent(motherId)) {
            throw new EmergencyException(HttpStatus.FORBIDDEN, "EMERG-009",
                    "Active LOCATION/SHARE consent is required");
        }

        List<CareGroupMember> members = careGroupMemberRepository
                .findAcceptedFamilyMembersForEmergencyAlerts(motherId);
        if (members.isEmpty()) {
            throw new EmergencyException(HttpStatus.CONFLICT, "EMERG-010",
                    "No eligible Family account is available");
        }

        String motherName = userRepository.findById(motherId)
                .map(user -> user.getName() == null || user.getName().isBlank()
                        ? "Mother"
                        : user.getName().trim())
                .orElse("Mother");
        UUID shareId = UUID.randomUUID();
        Instant sharedAt = Instant.now();
        String title = motherName + " đã gửi vị trí";
        String body = "Mother vừa chia sẻ vị trí hiện tại. Nhấn để xem tọa độ và mở chỉ đường.";

        Map<RecipientScope, CareGroupMember> recipients = new LinkedHashMap<>();
        for (CareGroupMember member : members) {
            recipients.putIfAbsent(new RecipientScope(member.getUserId(), member.getCareGroupId()), member);
        }

        int pushDeliveredCount = 0;
        for (RecipientScope recipient : recipients.keySet()) {
            List<DeviceToken> tokens = deviceTokenRepository
                    .findByUserIdAndActiveTrue(recipient.userId());
            Map<String, String> metadata = metadata(
                    shareId, motherId, motherName, request, sharedAt, recipient.careGroupId());
            NotificationRecord record = notificationRecordRepository.saveAndFlush(
                    NotificationRecord.builder()
                            .userId(recipient.userId())
                            .careGroupId(recipient.careGroupId())
                            .type(NotificationType.LOCATION_SHARE)
                            .title(title)
                            .body(body)
                            .referenceId(shareId)
                            .referenceType(REFERENCE_TYPE)
                            .status(NotificationRecordStatus.PROCESSING)
                            .attemptCount(0)
                            .metadata(metadata)
                            .build());

            Map<String, String> payload = new HashMap<>(metadata);
            payload.put("type", "LOCATION_SHARE");
            payload.put("referenceId", shareId.toString());
            payload.put("referenceType", REFERENCE_TYPE);
            payload.put("notificationId", record.getId().toString());
            payload.put("title", title);
            payload.put("body", body);

            int attempts = 0;
            String firstMessageId = null;
            boolean delivered = false;
            for (DeviceToken token : tokens) {
                FcmDeliveryResult result;
                try {
                    result = fcmService.sendWithRetry(
                            token.getToken(), title, body, payload, 3);
                } catch (RuntimeException exception) {
                    result = FcmDeliveryResult.failed("FCM_EXCEPTION", 1);
                }
                attempts += result.attempts();
                if (result.success()) {
                    delivered = true;
                    if (firstMessageId == null) firstMessageId = result.messageId();
                }
            }
            record.setAttemptCount(attempts);
            record.setFcmMessageId(firstMessageId);
            if (delivered) {
                pushDeliveredCount++;
                record.setStatus(NotificationRecordStatus.SENT);
                record.setSentAt(Instant.now());
            } else {
                record.setStatus(NotificationRecordStatus.FAILED);
                record.setFailedAt(Instant.now());
            }
            notificationRecordRepository.save(record);
            auditService.log(delivered ? AuditAction.NOTIFICATION_SENT : AuditAction.NOTIFICATION_FAILED,
                    motherId, "LocationShare", shareId.toString(),
                    Map.of("recipientUserId", recipient.userId().toString(),
                            "careGroupId", recipient.careGroupId().toString(),
                            "pushDelivered", delivered));
        }

        return new LocationShareResponse(
                shareId, recipients.size(), pushDeliveredCount, sharedAt);
    }

    private Map<String, String> metadata(
            UUID shareId,
            UUID motherId,
            String motherName,
            ShareLocationRequest request,
            Instant sharedAt,
            UUID careGroupId) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("shareId", shareId.toString());
        metadata.put("motherId", motherId.toString());
        metadata.put("motherName", motherName);
        metadata.put("latitude", request.latitude().toPlainString());
        metadata.put("longitude", request.longitude().toPlainString());
        metadata.put("sharedAt", sharedAt.toString());
        metadata.put("careGroupId", careGroupId.toString());
        return metadata;
    }

    private record RecipientScope(UUID userId, UUID careGroupId) {
    }
}
