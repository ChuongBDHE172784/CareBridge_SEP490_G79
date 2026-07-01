package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.WarnOrSuspendAccountResponse;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

// WSA-TC-INT-001, WSA-TC-INT-002
// Note: this codebase has no Testcontainers/real-DB integration harness (verified — zero usages
// project-wide, same finding as UC-100/UC-101). This test exercises the full Spring MVC + Security
// filter chain against a mocked service layer (established project convention — see
// ModerateContentIntegrationTest, ResolveReportIntegrationTest). It verifies HTTP-level wiring/response
// shape only; it does NOT verify real DB row persistence (users.suspended_until / moderation_actions
// columns) — that is covered instead at the service-unit-test level (WarnOrSuspendAccountServiceImplTest
// WSA-TC-202/WSA-TC-211). WSA-TC-INT-003 (atomicity rollback) requires a real transactional DB and is
// NOT implemented here — documented as an open gap, same honest treatment as UC-101's RES-TC-INT-004.
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class WarnOrSuspendAccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String URL = "/api/v1/admin/moderation/account-actions";
    private static final UUID TARGET_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000001");
    private static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-0000000000f2");

    // WSA-TC-INT-001: full API flow SUSPEND — 201, accountSuspended=true
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-0000000000f2", roles = "MODERATOR")
    void moderateAccount_suspend_fullStackReturns201() throws Exception {
        Instant expiresAt = Instant.parse("2026-07-15T00:00:00Z");
        WarnOrSuspendAccountResponse response = new WarnOrSuspendAccountResponse(
                UUID.randomUUID(), TARGET_USER_ID, ModerationActionType.SUSPEND, MODERATOR_ID,
                "Vi phạm lặp lại quy tắc cộng đồng", Instant.now(), expiresAt, true);
        when(moderationService.moderateAccount(any(), any())).thenReturn(response);

        String body = "{\"targetUserId\":\"" + TARGET_USER_ID
                + "\",\"actionType\":\"SUSPEND\",\"reason\":\"Vi phạm lặp lại quy tắc cộng đồng\","
                + "\"expiresAt\":\"" + expiresAt + "\"}";

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetUserId").value(TARGET_USER_ID.toString()))
                .andExpect(jsonPath("$.actionType").value("SUSPEND"))
                .andExpect(jsonPath("$.accountSuspended").value(true));
    }

    // WSA-TC-INT-002: full API flow WARN — 201, accountSuspended=false
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-0000000000f2", roles = "MODERATOR")
    void moderateAccount_warn_fullStackReturns201() throws Exception {
        WarnOrSuspendAccountResponse response = new WarnOrSuspendAccountResponse(
                UUID.randomUUID(), TARGET_USER_ID, ModerationActionType.WARN, MODERATOR_ID,
                "Ngôn từ không phù hợp", Instant.now(), null, false);
        when(moderationService.moderateAccount(any(), any())).thenReturn(response);

        String body = "{\"targetUserId\":\"" + TARGET_USER_ID
                + "\",\"actionType\":\"WARN\",\"reason\":\"Ngôn từ không phù hợp\"}";

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actionType").value("WARN"))
                .andExpect(jsonPath("$.accountSuspended").value(false))
                .andExpect(jsonPath("$.expiresAt").value(org.hamcrest.Matchers.nullValue()));
    }
}
