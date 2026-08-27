package com.carebridge.backend.directchat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// MEDI-TC-006, MEDI-TC-007, MEDI-TC-013, MEDI-TC-022 (HTTP level)
@WebMvcTest(
        value = DirectConversationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class DirectConversationControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IDirectConversationService conversationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID CONVERSATION_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID EXPERT_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
    private static final UUID FAMILY_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000004");
    private static final UUID STRANGER_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000099");
    private static final UUID MESSAGE_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    private static String markReadBody(UUID lastSeenMessageId) {
        return "{\"lastSeenMessageId\":\"" + lastSeenMessageId + "\"}";
    }

    // The direct find-or-create route was intentionally removed; it must remain unavailable.
    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000002", roles = "EXPERT")
    void findOrCreate_asExpert_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/direct-conversations/expert/" + EXPERT_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // MEDI-TC-007 — non-participant is rejected on every participant-scoped endpoint
    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000099", roles = "MOTHER")
    void getConversation_nonParticipant_forbidden() throws Exception {
        when(conversationService.getConversation(CONVERSATION_ID, STRANGER_ID))
                .thenThrow(DirectChatException.notParticipant());

        mockMvc.perform(get("/api/v1/direct-conversations/" + CONVERSATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("DCC-003"));
    }

    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000099", roles = "MOTHER")
    void markRead_nonParticipant_forbidden() throws Exception {
        when(conversationService.markRead(CONVERSATION_ID, STRANGER_ID, MESSAGE_ID))
                .thenThrow(DirectChatException.notParticipant());

        mockMvc.perform(patch("/api/v1/direct-conversations/" + CONVERSATION_ID + "/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(markReadBody(MESSAGE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("DCC-003"));
    }

    // MEDI-TC-013 — PATCH /read by a true non-participant -> 403 DCC-003, reused factory (not a
    // fictional MEDI-002 code — see TDS §10 v1.2 / L11).
    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000099", roles = "MOTHER")
    void markRead_trueThirdParty_returns403DCC003() throws Exception {
        when(conversationService.markRead(any(), any(), any())).thenThrow(DirectChatException.notParticipant());

        mockMvc.perform(patch("/api/v1/direct-conversations/" + CONVERSATION_ID + "/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(markReadBody(MESSAGE_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("DCC-003"));
    }

    // MEDI-TC-022 (HTTP half) — nonexistent conversationId -> 404 DCC-006
    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000002", roles = "EXPERT")
    void markRead_conversationNotFound_returns404DCC006() throws Exception {
        UUID randomConversationId = UUID.randomUUID();
        when(conversationService.markRead(org.mockito.ArgumentMatchers.eq(randomConversationId), any(), any()))
                .thenThrow(DirectChatException.conversationNotFound());

        mockMvc.perform(patch("/api/v1/direct-conversations/" + randomConversationId + "/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(markReadBody(MESSAGE_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DCC-006"));
    }

    // Regression guard — participant-scoped endpoints remain reachable for every chat role.
    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000002", roles = "EXPERT")
    void listMyConversations_asExpert_ok() throws Exception {
        when(conversationService.listMyConversations(EXPERT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/direct-conversations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000004", roles = "FAMILY")
    void listMyConversations_asFamily_ok() throws Exception {
        when(conversationService.listMyConversations(FAMILY_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/direct-conversations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000004", roles = "FAMILY")
    void getConversation_asFamilyNonParticipant_forbiddenByParticipantPolicy() throws Exception {
        when(conversationService.getConversation(CONVERSATION_ID, FAMILY_ID))
                .thenThrow(DirectChatException.notParticipant());

        mockMvc.perform(get("/api/v1/direct-conversations/" + CONVERSATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("DCC-003"));
    }

    @Test
    void listMyConversations_withoutAuthentication_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/direct-conversations"))
                .andExpect(status().isUnauthorized());
    }
}
