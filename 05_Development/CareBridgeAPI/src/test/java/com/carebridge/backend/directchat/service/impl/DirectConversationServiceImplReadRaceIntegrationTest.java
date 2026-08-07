package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.dto.response.UnreadSummaryResponse;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.integration.zegocloud.ZegoTokenDto;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * MEDI-TC-019 — race regression for the bug the user flagged in round 1 (TDS §6.5): a message
 * arriving after the client loaded its timeline, but before the client's PATCH /read lands, must
 * stay unread. Exercised through the real HTTP -> service -> Postgres stack, not mocks, so a
 * regression to Instant.now()-based cursors (the original v1.0 design) would actually fail this.
 */
@Import(MockMvcSecurityBuilderConfig.class)
@Transactional
class DirectConversationServiceImplReadRaceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private IDirectConversationService conversationService;

    @MockitoBean private IZegoCloudService zegoCloudService;

    private static final UUID MOTHER_ID = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    private static final UUID EXPERT_USER_ID = UUID.fromString("e1000000-0000-0000-0000-000000000002");
    private UUID conversationId;

    @BeforeEach
    void seed() {
        CanonicalUserFixture.insertUser(
                jdbcTemplate, MOTHER_ID, "Mother Race", "0900100001", "MOTHER");
        CanonicalUserFixture.insertUser(
                jdbcTemplate, EXPERT_USER_ID, "Expert Race", "0900100002", "EXPERT");
        // Canonical model: expert profile data lives on the users row itself.
        jdbcTemplate.update(
                "UPDATE users SET specialty = 'Sản khoa', verification_status = 'APPROVED', trust_status = 'ACTIVE' "
                        + "WHERE user_id = ?",
                EXPERT_USER_ID);
        conversationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO direct_conversations (conversation_id, mother_user_id, "
                        + "expert_user_id, status, created_at, last_activity_at) "
                        + "VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                conversationId, MOTHER_ID, EXPERT_USER_ID);
        when(zegoCloudService.generateToken(any(), any(), any()))
                .thenReturn(new ZegoTokenDto("room", "tok", 1L, Instant.now().plusSeconds(3600)));
    }

    @Test
    @WithMockUser(username = "e1000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void raceRegression_messageArrivingAfterTimelineLoad_staysUnreadAfterMarkRead() throws Exception {
        // t0 — Mother sends M1. Expert's client "loads the timeline" here: the only message it
        // could possibly have seen is M1 (M2 does not exist yet).
        String m1Response = mockMvc.perform(post("/api/v1/direct-conversations/" + conversationId + "/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientMessageId\":\"" + UUID.randomUUID() + "\",\"messageBody\":\"M1\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID m1Id = UUID.fromString(extractJsonField(m1Response, "messageId"));

        // t2 (> t0) — M2 arrives on the server BEFORE the Expert's PATCH /read request lands,
        // exactly the window described in TDS §6.5. The Expert's client still only knows about M1.
        mockMvc.perform(post("/api/v1/direct-conversations/" + conversationId + "/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientMessageId\":\"" + UUID.randomUUID() + "\",\"messageBody\":\"M2\"}"))
                .andExpect(status().isCreated());

        // t3 (> t2) — Expert's client calls PATCH /read with lastSeenMessageId = M1.id (the last
        // message it actually rendered), NOT "whatever is newest on the server now".
        mockMvc.perform(patch("/api/v1/direct-conversations/" + conversationId + "/read")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(EXPERT_USER_ID.toString()).roles("EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastSeenMessageId\":\"" + m1Id + "\"}"))
                .andExpect(status().isOk());

        Instant m1CreatedAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM direct_messages WHERE message_id = ?",
                Instant.class, m1Id);
        // Read state lives only in the cursor table now — the legacy
        // direct_conversations.expert_last_read_at column no longer exists.
        Instant expertLastReadAt = jdbcTemplate.queryForObject(
                """
                SELECT last_read_at
                  FROM direct_conversation_read_cursors
                 WHERE conversation_id = ? AND reader_user_id = ?
                """,
                Instant.class, conversationId, EXPERT_USER_ID);
        assertThat(expertLastReadAt).isEqualTo(m1CreatedAt); // cursor == M1.createdAt, never "now" at t3

        List<DirectConversationSummaryResponse> summaries = conversationService.listMyConversations(EXPERT_USER_ID);
        assertThat(summaries.get(0).getUnreadCount()).isEqualTo(1); // M2 still unread

        UnreadSummaryResponse unreadSummary = conversationService.getUnreadSummary(EXPERT_USER_ID);
        assertThat(unreadSummary.totalUnreadMessageCount()).isEqualTo(1);
    }

    @Test
    void compositeCursor_sameTimestampLaterMessage_staysUnread() {
        Instant sameTime = Instant.parse("2026-07-16T08:00:00Z");
        UUID first = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("20000000-0000-0000-0000-000000000002");
        jdbcTemplate.update("""
                INSERT INTO direct_messages
                    (message_id, conversation_id, sender_user_id,
                     client_message_id, message_type, message_body, created_at)
                VALUES (?, ?, ?, ?, 'TEXT', 'first', ?),
                       (?, ?, ?, ?, 'TEXT', 'second', ?)
                """, first, conversationId, MOTHER_ID, UUID.randomUUID(),
                java.sql.Timestamp.from(sameTime), second, conversationId,
                MOTHER_ID, UUID.randomUUID(), java.sql.Timestamp.from(sameTime));

        var cursor = conversationService.markRead(conversationId, EXPERT_USER_ID, first);

        assertThat(cursor.messageId()).isEqualTo(first);
        assertThat(conversationService.getUnreadSummary(EXPERT_USER_ID).totalUnreadMessageCount()).isEqualTo(1);
        assertThat(conversationService.listMyConversations(EXPERT_USER_ID).getFirst().getLastMessagePreview())
                .isEqualTo("second");
    }

    private static String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
