package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.integration.zegocloud.ZegoTokenDto;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MEDI-TC-014b, MEDI-TC-015 — end-to-end sendMessage() -> AFTER_COMMIT -> notification pipeline
 * against a real Postgres + the real @Async/@TransactionalEventListener wiring.
 *
 * <p>Deliberately NOT @Transactional: a test-managed transaction never truly commits (it always
 * rolls back at test end), so an AFTER_COMMIT listener would never fire and every assertion below
 * would see 0 rows. Each test therefore seeds its own fresh random user/conversation ids instead
 * (no shared fixed ids, no collisions across methods) and genuinely commits, same as
 * DirectChatIntegrationTest.
 *
 * <p>Runs with the real (thread-pool) async executor — forcing a same-thread SyncTaskExecutor was
 * tried and reverted: it makes the AFTER_COMMIT listener run on the same thread that just committed
 * the outer transaction, inside the window before JpaTransactionManager unbinds the thread-local
 * EntityManager, so notifyNewMessage()'s own @Transactional wrongly "participates" in the
 * already-committed outer transaction instead of starting its own — its final UPDATE then never
 * commits. That is a same-thread test-harness artifact that cannot occur in production, where
 * @Async genuinely dispatches to a different thread with no thread-local transaction state. The
 * assertions below poll for the listener's terminal write instead of assuming it is synchronous.
 */
@Import(MockMvcSecurityBuilderConfig.class)
class DirectMessageServiceImplNotificationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;
    @MockitoBean private FcmService fcmService;
    @MockitoSpyBean private AuditService auditService;

    private UUID motherId;
    private UUID expertUserId;
    private UUID conversationId;

    @BeforeEach
    void seed() {
        motherId = UUID.randomUUID();
        expertUserId = UUID.randomUUID();
        String phoneSuffix = String.valueOf(System.nanoTime()).substring(3, 11);
        CanonicalUserFixture.insertUser(
                jdbcTemplate, motherId, "Mother Notif", "07" + phoneSuffix, "MOTHER");
        CanonicalUserFixture.insertUser(
                jdbcTemplate, expertUserId, "Expert Notif", "06" + phoneSuffix, "EXPERT");
        // Canonical model: expert profile data lives on the users row itself.
        jdbcTemplate.update(
                "UPDATE users SET specialty = 'Sản khoa', verification_status = 'APPROVED', trust_status = 'ACTIVE' "
                        + "WHERE user_id = ?",
                expertUserId);
        conversationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO direct_conversations (conversation_id, mother_user_id, "
                        + "expert_user_id, status, created_at, last_activity_at) "
                        + "VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                conversationId, motherId, expertUserId);
        jdbcTemplate.update(
                "INSERT INTO device_tokens (id, user_id, token, platform, active, created_at, updated_at) "
                        + "VALUES (?, ?, 'expert-fcm-token', 'ANDROID', true, now(), now())",
                UUID.randomUUID(), expertUserId);
        when(zegoCloudService.generateToken(any(), any(), any()))
                .thenReturn(new ZegoTokenDto("room", "tok", 1L, Instant.now().plusSeconds(3600)));
    }

    private String sendMessage(String clientMessageId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/direct-conversations/" + conversationId + "/messages")
                        .with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientMessageId\":\"" + clientMessageId + "\",\"messageBody\":\"" + body + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    private Integer notificationRowCount(UUID messageId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_records WHERE reference_id = ? AND type = 'MESSAGE'",
                Integer.class, messageId);
    }

    private static String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    /** Waits for the real (thread-pool) @Async listener to reach a terminal SENT/FAILED write. */
    private void awaitNotificationTerminal(UUID messageId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            Integer terminalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM notification_records WHERE reference_id = ? "
                            + "AND (sent_at IS NOT NULL OR failed_at IS NOT NULL)",
                    Integer.class, messageId);
            if (terminalCount != null && terminalCount > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("notification_records for message " + messageId
                + " did not reach a terminal state within 5s");
    }

    // MEDI-TC-014b step 1-3 — happy path: message commits, exactly 1 SENT record for the recipient
    @Test
    void sendMessage_success_createsExactlyOneSentNotificationRecord() throws Exception {
        when(fcmService.sendWithRetry(eq("expert-fcm-token"), any(), any(), anyInt()))
                .thenReturn(FcmDeliveryResult.success("fcm-msg-1", 1));

        String response = sendMessage(UUID.randomUUID().toString(), "Xin chao");
        UUID messageId = UUID.fromString(extractJsonField(response, "messageId"));
        awaitNotificationTerminal(messageId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM direct_messages WHERE message_id = ?",
                Integer.class, messageId)).isEqualTo(1);
        assertThat(notificationRowCount(messageId)).isEqualTo(1);
        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM notification_records WHERE reference_id = ?", Integer.class, messageId);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_records WHERE reference_id = ?", String.class, messageId);
        assertThat(status).isEqualTo("SENT");
        assertThat(attemptCount).isEqualTo(1);
        String body = jdbcTemplate.queryForObject(
                "SELECT body FROM notification_records WHERE reference_id = ?", String.class, messageId);
        assertThat(body).doesNotContain("Xin chao"); // C4 — never the raw message content
    }

    // MEDI-TC-014b step 5 — sendWithRetry throws: message still commits, exactly 1 FAILED row,
    // attemptCount sentinel == 0 (distinct oracle from the graceful-failure branch below).
    @Test
    void sendMessage_fcmThrows_messageStillCommitted_oneFailedRecordWithZeroAttempts() throws Exception {
        when(fcmService.sendWithRetry(eq("expert-fcm-token"), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("network down"));

        String response = sendMessage(UUID.randomUUID().toString(), "should still be saved");
        UUID messageId = UUID.fromString(extractJsonField(response, "messageId"));
        awaitNotificationTerminal(messageId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM direct_messages WHERE message_id = ?",
                Integer.class, messageId)).isEqualTo(1);
        assertThat(notificationRowCount(messageId)).isEqualTo(1);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_records WHERE reference_id = ?", String.class, messageId);
        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM notification_records WHERE reference_id = ?", Integer.class, messageId);
        assertThat(status).isEqualTo("FAILED");
        assertThat(attemptCount).isEqualTo(0);
        // Oracle is "AuditService.log gọi đúng 1 lần cho record này" (Test-Spec) — verified via spy,
        // not a persisted audit_logs row: AuditEligibilityPolicy (pre-existing, out of this feature's
        // scope) does not include NOTIFICATION_FAILED in its sensitive-actions allowlist, so the call
        // is legitimately a no-op at the persistence layer.
        Mockito.verify(auditService, Mockito.times(1))
                .log(eq(AuditAction.NOTIFICATION_FAILED), eq(expertUserId), eq("NotificationRecord"), any(), eq("MESSAGE"));
    }

    // MEDI-TC-014b step 6 — graceful failure (FcmDeliveryResult.failed, no exception): attemptCount
    // == delivery.attempts() exactly (3), a DIFFERENT value from the exception-branch sentinel above.
    @Test
    void sendMessage_fcmGracefulFailure_attemptCountFromDeliveryResult() throws Exception {
        when(fcmService.sendWithRetry(eq("expert-fcm-token"), any(), any(), anyInt()))
                .thenReturn(FcmDeliveryResult.failed("TOKEN_EXPIRED", 3));

        String response = sendMessage(UUID.randomUUID().toString(), "graceful failure case");
        UUID messageId = UUID.fromString(extractJsonField(response, "messageId"));
        awaitNotificationTerminal(messageId);

        assertThat(notificationRowCount(messageId)).isEqualTo(1);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM notification_records WHERE reference_id = ?", String.class, messageId);
        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM notification_records WHERE reference_id = ?", Integer.class, messageId);
        assertThat(status).isEqualTo("FAILED");
        assertThat(attemptCount).isEqualTo(3);
    }

    // MEDI-TC-015 — retry with the SAME clientMessageId never publishes the event a second time,
    // so the listener never runs twice and only 1 notification_records row ever exists.
    @Test
    void sendMessage_retrySameClientMessageId_doesNotCreateSecondNotification() throws Exception {
        when(fcmService.sendWithRetry(eq("expert-fcm-token"), any(), any(), anyInt()))
                .thenReturn(FcmDeliveryResult.success("fcm-msg-x", 1));
        String clientMessageId = UUID.randomUUID().toString();

        String first = sendMessage(clientMessageId, "Retry body");
        UUID messageId = UUID.fromString(extractJsonField(first, "messageId"));
        awaitNotificationTerminal(messageId);
        String second = sendMessage(clientMessageId, "Retry body");
        UUID messageId2 = UUID.fromString(extractJsonField(second, "messageId"));

        assertThat(messageId2).isEqualTo(messageId); // same row returned, no duplicate message
        assertThat(notificationRowCount(messageId)).isEqualTo(1);
        org.mockito.Mockito.verify(fcmService, org.mockito.Mockito.times(1))
                .sendWithRetry(eq("expert-fcm-token"), any(), any(), anyInt());
    }
}
