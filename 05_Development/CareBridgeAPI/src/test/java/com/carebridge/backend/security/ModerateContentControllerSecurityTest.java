package com.carebridge.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.ModerateContentResponse;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// MOD-TC-113, MOD-TC-114, MOD-TC-115, MOD-TC-118, MOD-TC-SEC-001
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerateContentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String ACTIONS_URL = "/api/v1/admin/moderation/actions";
    private static final UUID QUESTION_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    private static String validBody() {
        return "{\"targetId\":\"" + QUESTION_ID + "\",\"targetType\":\"QUESTION\",\"actionType\":\"APPROVE\"}";
    }

    // MOD-TC-113: Non-MODERATOR bị 403 (NOT MOD-004 — see Logic Issue L4).
    // Note: this endpoint is denied at the URL-matcher level in SecurityConfig (defense-in-depth,
    // ADR-002), which runs in the AuthorizationFilter *before* DispatcherServlet — the request never
    // reaches GlobalExceptionHandler, so the response body is empty (verified — matches the existing
    // codebase convention in AdminContentControllerTest/ModerationControllerSecurityTest, which also
    // only assert status().isForbidden() for role-denied writes, never a JSON error body).
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000099", roles = "MOTHER")
    void moderateContent_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    // MOD-TC-114: SYSTEM_ADMIN has no implicit access — also 403 (Logic Issue L3, no RoleHierarchy bean)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000098", roles = "SYSTEM_ADMIN")
    void moderateContent_asSystemAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    // MOD-TC-118: CONTENT_ADMIN bị 403 — parity với Auth Matrix §16 (ContentItem mutation belongs elsewhere)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000097", roles = "CONTENT_ADMIN")
    void moderateContent_asContentAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    // MOD-TC-115: No JWT → 401 (bodiless — NOT IAM-001/MOD-006, see Logic Issue L4)
    @Test
    void moderateContent_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(post(ACTIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    // MOD-TC-SEC-001: SQL injection in `reason` field does not affect DB — handled as plain TEXT param
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void moderateContent_sqlInjectionInReason_handledAsLiteralText() throws Exception {
        String maliciousReason = "x'; DROP TABLE moderation_actions;--";
        when(moderationService.moderateContent(any(), any())).thenReturn(new ModerateContentResponse(
                UUID.randomUUID(), QUESTION_ID, ReportTargetType.QUESTION, ModerationActionType.HIDE,
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"), maliciousReason, Instant.now(), "HIDDEN"));

        String body = "{\"targetId\":\"" + QUESTION_ID
                + "\",\"targetType\":\"QUESTION\",\"actionType\":\"HIDE\",\"reason\":\"" + maliciousReason + "\"}";

        mockMvc.perform(post(ACTIONS_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value(maliciousReason));
    }
}
