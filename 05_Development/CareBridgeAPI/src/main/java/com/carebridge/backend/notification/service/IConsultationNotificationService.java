package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.ConsultationNotificationEventType;
import com.carebridge.backend.notification.dto.ConsultationNotificationPayload;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IConsultationNotificationService {

    List<NotificationRecordResponse> sendConsultationNotification(ConsultationNotificationPayload payload);

    List<UUID> resolveRecipients(UUID motherUserId, UUID expertUserId, ConsultationNotificationEventType eventType);

    Map<String, String> buildPayload(ConsultationNotificationPayload payload);

    String buildDeepLink(UUID consultationId, ConsultationNotificationEventType eventType);
}
