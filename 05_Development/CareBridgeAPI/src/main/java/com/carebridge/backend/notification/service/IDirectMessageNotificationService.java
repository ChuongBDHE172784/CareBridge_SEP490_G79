package com.carebridge.backend.notification.service;

import java.util.UUID;

/**
 * ADR-MEDI-004 — called only from {@code DirectMessageNotificationListener}
 * ({@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}), never synchronously from
 * {@code DirectMessageServiceImpl.sendMessage()} (constraint C3). Idempotent at the DB layer
 * (constraint C10): a duplicate call for the same (recipientUserId, messageId) is a no-op, not a
 * second FCM push. Every call that gets past the idempotency check ends with exactly one
 * {@code NotificationRecord} row, SENT or FAILED — an FCM error is never swallowed silently
 * (constraint C11).
 */
public interface IDirectMessageNotificationService {

    void notifyNewMessage(UUID recipientUserId, UUID senderUserId, UUID conversationId, UUID messageId);
}
