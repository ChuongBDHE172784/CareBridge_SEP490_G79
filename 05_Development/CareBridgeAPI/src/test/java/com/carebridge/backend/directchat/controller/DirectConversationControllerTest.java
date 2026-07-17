package com.carebridge.backend.directchat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.directchat.service.IDirectConversationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
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

// MEDI-TC-023 — missing/null lastSeenMessageId in PATCH /read body
@WebMvcTest(
        value = DirectConversationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class DirectConversationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IDirectConversationService conversationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID CONVERSATION_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    // Case A — field missing entirely
    @Test
    @WithMockUser(username = "dddddddd-0000-0000-0000-000000000002", roles = "MOTHER")
    void markRead_missingLastSeenMessageId_returns400ValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/direct-conversations/" + CONVERSATION_ID + "/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        org.mockito.Mockito.verifyNoInteractions(conversationService);
    }

    // Case B — field present but null
    @Test
    @WithMockUser(username = "dddddddd-0000-0000-0000-000000000002", roles = "MOTHER")
    void markRead_nullLastSeenMessageId_returns400ValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/direct-conversations/" + CONVERSATION_ID + "/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastSeenMessageId\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        org.mockito.Mockito.verify(conversationService, org.mockito.Mockito.never()).markRead(any(), any(), any());
    }

    // Sanity — a valid body actually reaches the service (isolates the two failure cases above from a
    // controller wiring bug that would make everything 400 regardless of body content).
    @Test
    @WithMockUser(username = "dddddddd-0000-0000-0000-000000000002", roles = "MOTHER")
    void markRead_validLastSeenMessageId_reachesService() throws Exception {
        UUID messageId = UUID.randomUUID();
        org.mockito.Mockito.when(conversationService.markRead(any(), any(), any()))
                .thenReturn(new com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository.ReadCursor(
                        java.time.Instant.parse("2026-07-16T08:00:00Z"), messageId));

        mockMvc.perform(patch("/api/v1/direct-conversations/" + CONVERSATION_ID + "/read")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastSeenMessageId\":\"" + messageId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cursorAt").value("2026-07-16T08:00:00Z"))
                .andExpect(jsonPath("$.data.cursorMessageId").value(messageId.toString()));
    }
}
