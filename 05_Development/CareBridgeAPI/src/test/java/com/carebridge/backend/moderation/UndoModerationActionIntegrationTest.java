package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.UndoModerationActionResponse;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// UNDO-TC-INT-001: full HTTP stack (Security -> Controller -> Service, mocked persistence) —
// follows this module's existing "integration test" convention (WebMvcTest + mocked service,
// see ModerationQueueIntegrationTest.java); DB-level assertions (answer_count, append-only) are
// already covered at the unit level (UNDO-TC-004/005/014) since this package has no Testcontainers.
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UndoModerationActionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID ACTION_ID = UUID.fromString("33333333-0000-0000-0000-000000000001");
    private static final String UNDO_URL = "/api/v1/admin/moderation/actions/" + ACTION_ID + "/undo";

    // UNDO-TC-INT-001 (happy path leg): undo succeeds over the full HTTP stack, resultingStatus=PENDING
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void undoModerationAction_happyPath_returns201WithPendingStatus() throws Exception {
        UndoModerationActionResponse response = new UndoModerationActionResponse(
                UUID.randomUUID(), ACTION_ID, UUID.randomUUID(), ReportTargetType.QUESTION,
                UUID.randomUUID(), Instant.now(), "PENDING");
        when(moderationService.undoModerationAction(any(), any())).thenReturn(response);

        mockMvc.perform(post(UNDO_URL).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultingStatus").value("PENDING"))
                .andExpect(jsonPath("$.originalActionId").value(ACTION_ID.toString()));
    }

    // UNDO-TC-INT-001 (repeat-call leg): calling undo a second time on the same actionId now fails
    // the "most recent action" guard (ADR-002 guard 1) — surfaced as 409
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void undoModerationAction_calledTwiceOnSameActionId_secondCallReturns409() throws Exception {
        when(moderationService.undoModerationAction(any(), any()))
                .thenThrow(ModerationException.undoNotMostRecentAction(ACTION_ID));

        mockMvc.perform(post(UNDO_URL).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MOD-029"));
    }
}
