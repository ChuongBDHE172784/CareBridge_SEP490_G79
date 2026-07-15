package com.carebridge.backend.directchat.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.integration.zegocloud.ZegoTokenDto;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DCC-TC-INT-001 (Test-Spec v1.2). Exercises the real Postgres schema (§5.2 TDS): NOT
 * NULL / CHECK constraints, FK, unique constraints, and the full find-or-create -> send ->
 * timeline -> call lifecycle against a live database, not mocks.
 *
 * <p>Compiles and is structurally complete but could not be executed in this sandbox — no
 * Docker environment available (verified: {@code docker info} fails to reach the daemon).
 * Must be run on a machine with Docker before this feature is considered fully verified.
 */
@Import(MockMvcSecurityBuilderConfig.class)
class DirectChatIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DirectConversationRepository conversationRepository;
    @Autowired private DirectMessageRepository messageRepository;
    @Autowired private ConversationCallRepository callRepository;

    @MockitoBean private IZegoCloudService zegoCloudService;

    private static final UUID MOTHER_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID EXPERT_USER_ID = UUID.fromString("d1000000-0000-0000-0000-000000000002");
    private static UUID expertProfileId;

    @BeforeEach
    void seedUsers() {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Mother Test', '0900000001', 'MOTHER', true, false, now(), now())",
                MOTHER_ID);
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Expert Test', '0900000002', 'EXPERT', true, false, now(), now())",
                EXPERT_USER_ID);
        expertProfileId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO expert_profiles (expert_profile_id, user_id, specialty, verification_status, created_at, updated_at) "
                        + "VALUES (?, ?, 'Sản khoa', 'APPROVED', now(), now())",
                expertProfileId, EXPERT_USER_ID);

        when(zegoCloudService.generateToken(any(), any(), any()))
                .thenReturn(new ZegoTokenDto("room", "tok", 1L, Instant.now().plusSeconds(3600)));
    }

    @Test
    @WithMockUser(username = "d1000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void fullLifecycle_findOrCreate_sendMessage_timeline_callLifecycle() throws Exception {
        // Step 1 — find-or-create
        mockMvc.perform(post("/api/v1/direct-conversations/expert/" + expertProfileId).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.motherUserId").value(MOTHER_ID.toString()))
                .andExpect(jsonPath("$.data.expertAvailable").value(true));
        assertThat(conversationRepository.findByMotherUserIdAndExpertUserId(MOTHER_ID, EXPERT_USER_ID)).isPresent();
        UUID conversationId = conversationRepository
                .findByMotherUserIdAndExpertUserId(MOTHER_ID, EXPERT_USER_ID).orElseThrow().getId();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'DIRECT_CONVERSATION_OPENED'", Integer.class))
                .isGreaterThanOrEqualTo(1);

        // Step 2 — NOT NULL violation on client_message_id (BR-DCC-005 siết DDL)
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO direct_messages (message_id, conversation_id, sender_user_id, message_type, message_body, created_at) "
                        + "VALUES (gen_random_uuid(), ?, ?, 'TEXT', 'hi', now())",
                conversationId, MOTHER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Step 3 — CHECK violation on empty message_body
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO direct_messages (message_id, conversation_id, sender_user_id, client_message_id, message_type, message_body, created_at) "
                        + "VALUES (gen_random_uuid(), ?, ?, gen_random_uuid(), 'TEXT', '   ', now())",
                conversationId, MOTHER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Step 4 — CHECK violation on message_type != TEXT (BR-DCC-016)
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO direct_messages (message_id, conversation_id, sender_user_id, client_message_id, message_type, message_body, created_at) "
                        + "VALUES (gen_random_uuid(), ?, ?, gen_random_uuid(), 'FILE', 'a file', now())",
                conversationId, MOTHER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Step 5 — CHECK violation on bogus call_status
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO conversation_calls (call_id, conversation_id, initiated_by_user_id, call_type, call_status, zego_room_id, initiated_at, created_at) "
                        + "VALUES (gen_random_uuid(), ?, ?, 'VOICE', 'BOGUS', 'room', now(), now())",
                conversationId, MOTHER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Step 6 — CHECK violation: ENDED requires answered_at (chk_conversation_calls_ended_requires_answered)
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO conversation_calls (call_id, conversation_id, initiated_by_user_id, call_type, call_status, zego_room_id, initiated_at, created_at) "
                        + "VALUES (gen_random_uuid(), ?, ?, 'VOICE', 'ENDED', 'room', now(), now())",
                conversationId, MOTHER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Step 7 — send a real message, audit log succeeds (catches missing CHECK-widen migration)
        String clientMessageId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/direct-conversations/" + conversationId + "/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientMessageId\":\"" + clientMessageId + "\",\"messageBody\":\"Chào bác sĩ\"}"))
                .andExpect(status().isCreated());
        assertThat(messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                conversationId, MOTHER_ID, UUID.fromString(clientMessageId))).isPresent();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'DIRECT_MESSAGE_SENT'", Integer.class))
                .isGreaterThanOrEqualTo(1);

        // Step 8 — initiate -> answer -> end call lifecycle; assert duration computed server-side
        String callResponse = mockMvc.perform(post("/api/v1/direct-conversations/" + conversationId + "/calls")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"callType\":\"VOICE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID callId = UUID.fromString(extractJsonField(callResponse, "callId"));

        mockMvc.perform(patch("/api/v1/direct-conversations/" + conversationId + "/calls/" + callId + "/ringing")
                        .with(csrf()).with(user2()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/direct-conversations/" + conversationId + "/calls/" + callId + "/answer")
                        .with(csrf()).with(user2()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/direct-conversations/" + conversationId + "/calls/" + callId + "/end")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.callStatus").value("ENDED"))
                .andExpect(jsonPath("$.data.durationSeconds").exists());
        assertThat(callRepository.findById(callId).orElseThrow().getDurationSeconds()).isNotNull();

        // Step 9 — GET /timeline reflects both the message and the call, real UNION ALL query
        mockMvc.perform(get("/api/v1/direct-conversations/" + conversationId + "/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        // Step 10 — revoke Expert, Mother still reads (200), Mother blocked from writing (409 DCC-010)
        jdbcTemplate.update("UPDATE expert_profiles SET verification_status = 'PENDING' WHERE expert_profile_id = ?",
                expertProfileId);
        mockMvc.perform(get("/api/v1/direct-conversations/" + conversationId + "/timeline"))
                .andExpect(status().isOk());
        long messageCountBefore = countMessages(conversationId);
        mockMvc.perform(post("/api/v1/direct-conversations/" + conversationId + "/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientMessageId\":\"" + UUID.randomUUID() + "\",\"messageBody\":\"after revoke\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DCC-010"));
        assertThat(countMessages(conversationId)).isEqualTo(messageCountBefore);
    }

    private long countMessages(UUID conversationId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM direct_messages WHERE conversation_id = ?", Long.class, conversationId);
        return count == null ? 0 : count;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user2() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user(EXPERT_USER_ID.toString()).roles("EXPERT");
    }

    private static String extractJsonField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}
