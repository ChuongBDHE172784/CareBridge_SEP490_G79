package com.carebridge.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * MEDI-TC-020 — calling notifyNewMessage() twice, sequentially, for the exact same
 * (recipient, messageId) must never create a second notification_records row nor call FCM twice.
 * This is the scenario the clientMessageId early-return in sendMessage() cannot see: it guards
 * against message duplication, not against the AFTER_COMMIT listener itself being re-invoked
 * (ADR-MEDI-004 v1.1 mục 3).
 */
class DirectMessageNotificationServiceIdempotencyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private IDirectMessageNotificationService notificationService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private FcmService fcmService;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void notifyNewMessage_calledTwiceSequentiallyForSamePair_exactlyOneRow_fcmCalledOnce() {
        UUID recipientId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String phoneSuffix = String.valueOf(System.nanoTime()).substring(3, 11);

        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Idempotency Recipient', ?, 'EXPERT', true, false, now(), now())",
                recipientId, "05" + phoneSuffix);
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Idempotency Sender', ?, 'MOTHER', true, false, now(), now())",
                senderId, "04" + phoneSuffix);
        jdbcTemplate.update(
                "INSERT INTO device_tokens (id, user_id, token, platform, active, created_at, updated_at) "
                        + "VALUES (?, ?, 'idempotency-token', 'ANDROID', true, now(), now())",
                UUID.randomUUID(), recipientId);
        when(fcmService.sendWithRetry(eq("idempotency-token"), any(), any(), anyInt()))
                .thenReturn(FcmDeliveryResult.success("fcm-idem-1", 1));

        notificationService.notifyNewMessage(recipientId, senderId, conversationId, messageId);
        notificationService.notifyNewMessage(recipientId, senderId, conversationId, messageId); // simulated replay

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_records WHERE user_id = ? AND reference_id = ? AND type = 'MESSAGE'",
                Integer.class, recipientId, messageId);
        assertThat(rowCount).isEqualTo(1);
        verify(fcmService, times(1)).sendWithRetry(eq("idempotency-token"), any(), any(), anyInt());
    }
}
