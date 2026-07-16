package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.dto.response.TimelinePageResponse;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.directchat.service.IDirectMessageService;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * MEDI-TC-016 — Expert loses APPROVED mid-conversation: new writes (and therefore new
 * notifications) are blocked, but the Mother's existing history/unread stays fully readable.
 * Regression on ADR-DCC-007 (unchanged) combined with the new notification path (ADR-MEDI-004 mục 6).
 */
class DirectMessageServiceImplExpertRevokedTest extends AbstractPostgresIntegrationTest {

    @Autowired private IDirectMessageService directMessageService;
    @Autowired private IDirectConversationService directConversationService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;
    @MockitoBean private FcmService fcmService;

    @Test
    void expertSuspendedMidConversation_blocksNewMessage_keepsHistoryAndUnreadReadable() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        String phoneSuffix = String.valueOf(System.nanoTime()).substring(3, 11);
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Mother Revoked', ?, 'MOTHER', true, false, now(), now())",
                motherId, "07" + phoneSuffix);
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Expert Revoked', ?, 'EXPERT', true, false, now(), now())",
                expertUserId, "06" + phoneSuffix);
        UUID expertProfileId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO expert_profiles (expert_profile_id, user_id, specialty, verification_status, created_at, updated_at) "
                        + "VALUES (?, ?, 'Sản khoa', 'APPROVED', now(), now())",
                expertProfileId, expertUserId);
        UUID conversationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO direct_conversations (conversation_id, mother_user_id, expert_user_id, status, created_at, last_activity_at) "
                        + "VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                conversationId, motherId, expertUserId);
        UUID priorMessageId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO direct_messages (message_id, conversation_id, sender_user_id, client_message_id, "
                        + "message_type, message_body, created_at) VALUES (?, ?, ?, ?, 'TEXT', 'Old history', now())",
                priorMessageId, conversationId, expertUserId, UUID.randomUUID());
        jdbcTemplate.update(
                "UPDATE direct_conversations SET last_activity_at = now() WHERE conversation_id = ?", conversationId);

        // Preconditions done — now revoke.
        jdbcTemplate.update(
                "UPDATE expert_profiles SET verification_status = 'SUSPENDED' WHERE user_id = ?", expertUserId);

        // Step 1 — Mother tries to send a new message: must be blocked, nothing persisted, no notification.
        SendDirectMessageRequest request = new SendDirectMessageRequest();
        request.setClientMessageId(UUID.randomUUID());
        request.setMessageBody("Are you still there?");
        assertThatThrownBy(() -> directMessageService.sendMessage(conversationId, motherId, request))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));

        Integer messageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM direct_messages WHERE conversation_id = ?", Integer.class, conversationId);
        assertThat(messageCount).isEqualTo(1); // only the pre-existing history message, no new insert
        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_records WHERE metadata->>'conversationId' = ?",
                Integer.class, conversationId.toString());
        assertThat(notificationCount).isEqualTo(0);

        // Step 2 — Mother's conversation list still shows FX-C1 with old history/unread intact,
        // but expertAvailable flips to false.
        List<DirectConversationSummaryResponse> conversations = directConversationService.listMyConversations(motherId);
        DirectConversationSummaryResponse summary = conversations.stream()
                .filter(c -> c.getConversationId().equals(conversationId))
                .findFirst()
                .orElseThrow();
        assertThat(summary.isExpertAvailable()).isFalse();
        assertThat(summary.getLastMessagePreview()).isEqualTo("Old history");
        assertThat(summary.getUnreadCount()).isGreaterThanOrEqualTo(1);

        // Step 3 — Mother can still read the timeline.
        TimelinePageResponse timeline = directMessageService.getTimeline(conversationId, motherId, null, null, 20);
        assertThat(timeline.getItems()).hasSize(1);
        assertThat(timeline.getItems().get(0).getMessageId()).isEqualTo(priorMessageId);
    }
}
