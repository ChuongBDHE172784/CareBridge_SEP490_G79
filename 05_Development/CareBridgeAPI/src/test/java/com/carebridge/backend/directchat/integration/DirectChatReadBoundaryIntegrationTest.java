package com.carebridge.backend.directchat.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.entity.CallType;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.service.IConversationCallService;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.directchat.service.IDirectMessageService;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DirectChatReadBoundaryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private IDirectConversationService conversationService;
    @Autowired private IDirectMessageService messageService;
    @Autowired private IConversationCallService callService;
    @MockitoBean private IZegoCloudService zegoCloudService;

    @Test
    void readsUseVerificationOnlyWhileNewInteractionsRequireFullEligibility() {
        Fixture fixture = seedFixture();

        jdbcTemplate.update(
                "UPDATE expert_profiles SET trust_status='REVOKED' WHERE expert_profile_id=?",
                fixture.expertProfileId());
        assertThat(conversationService.getConversation(
                        fixture.conversationId(), fixture.motherId())
                .isExpertAvailable())
                .isFalse();
        assertThat(messageService.getTimeline(
                        fixture.conversationId(), fixture.motherId(), null, null, 20)
                .getItems())
                .hasSize(1);
        conversationService.markRead(
                fixture.conversationId(), fixture.motherId(), fixture.messageId());
        conversationService.getConversation(fixture.conversationId(), fixture.expertUserId());
        messageService.getTimeline(
                fixture.conversationId(), fixture.expertUserId(), null, null, 20);
        conversationService.markRead(
                fixture.conversationId(), fixture.expertUserId(), fixture.messageId());

        assertThatThrownBy(() -> messageService.sendMessage(
                        fixture.conversationId(), fixture.motherId(), messageRequest()))
                .isInstanceOfSatisfying(
                        DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));
        assertThatThrownBy(() -> callService.initiateCall(
                        fixture.conversationId(), fixture.motherId(), CallType.VOICE))
                .isInstanceOfSatisfying(
                        DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));

        jdbcTemplate.update(
                """
                UPDATE expert_profiles
                   SET verification_status='REJECTED', trust_status='ACTIVE'
                 WHERE expert_profile_id=?
                """,
                fixture.expertProfileId());
        conversationService.getConversation(fixture.conversationId(), fixture.motherId());
        messageService.getTimeline(
                fixture.conversationId(), fixture.motherId(), null, null, 20);
        conversationService.markRead(
                fixture.conversationId(), fixture.motherId(), fixture.messageId());

        assertExpertReadBlocked(() -> conversationService.getConversation(
                fixture.conversationId(), fixture.expertUserId()));
        assertExpertReadBlocked(() -> messageService.getTimeline(
                fixture.conversationId(), fixture.expertUserId(), null, null, 20));
        assertExpertReadBlocked(() -> conversationService.markRead(
                fixture.conversationId(), fixture.expertUserId(), fixture.messageId()));
    }

    private void assertExpertReadBlocked(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-002"));
    }

    private Fixture seedFixture() {
        UUID motherId = UUID.randomUUID();
        UUID expertUserId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        seedUser(motherId, "Read Mother", "MOTHER");
        seedUser(expertUserId, "Read Expert", "EXPERT");
        jdbcTemplate.update("""
                INSERT INTO expert_profiles
                    (expert_profile_id, user_id, specialty, verification_status, trust_status,
                     created_at, updated_at)
                VALUES (?, ?, 'Sản khoa', 'APPROVED', 'ACTIVE', now(), now())
                """, expertProfileId, expertUserId);
        jdbcTemplate.update("""
                INSERT INTO direct_conversations
                    (conversation_id, mother_user_id, expert_user_id, status,
                     created_at, last_activity_at)
                VALUES (?, ?, ?, 'ACTIVE', now(), now())
                """, conversationId, motherId, expertUserId);
        jdbcTemplate.update("""
                INSERT INTO direct_messages
                    (message_id, conversation_id, sender_user_id, client_message_id,
                     message_type, message_body, created_at)
                VALUES (?, ?, ?, ?, 'TEXT', 'Existing history', now())
                """, messageId, conversationId, motherId, UUID.randomUUID());
        return new Fixture(motherId, expertUserId, expertProfileId, conversationId, messageId);
    }

    private void seedUser(UUID id, String name, String role) {
        jdbcTemplate.update("""
                INSERT INTO users
                    (user_id, full_name, phone, role, enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, ?, true, false, now(), now())
                """, id, name, uniquePhone(), role);
    }

    private static SendDirectMessageRequest messageRequest() {
        SendDirectMessageRequest request = new SendDirectMessageRequest();
        request.setClientMessageId(UUID.randomUUID());
        request.setMessageBody("Blocked write");
        return request;
    }

    private static String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private record Fixture(
            UUID motherId,
            UUID expertUserId,
            UUID expertProfileId,
            UUID conversationId,
            UUID messageId) {
    }
}
