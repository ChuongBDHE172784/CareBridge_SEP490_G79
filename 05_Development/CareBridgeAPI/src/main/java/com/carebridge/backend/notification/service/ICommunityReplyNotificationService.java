package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import java.util.UUID;

public interface ICommunityReplyNotificationService {

    NotificationRecordResponse sendReplyNotification(UUID questionId, UUID answerId, UUID answererId, String answerPreview);

    void muteQuestion(UUID userId, UUID questionId);

    void unmuteQuestion(UUID userId, UUID questionId);
}
