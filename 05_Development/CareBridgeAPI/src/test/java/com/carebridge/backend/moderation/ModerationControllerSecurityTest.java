package com.carebridge.backend.moderation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// MOD-TC-006, MOD-TC-009, MOD-TC-SEC-001, MOD-TC-SEC-002, MOD-TC-SEC-003
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String QUEUE_URL = "/api/v1/admin/moderation/queue";

    // MOD-TC-006: ROLE_MOTHER cannot access moderation queue → 403 (ADR-002, BR-RBAC-001)
    // OWASP A01:2021 — Broken Access Control
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getQueue_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(QUEUE_URL))
                .andExpect(status().isForbidden());
    }

    // MOD-TC-009: No JWT → 401
    @Test
    void getQueue_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get(QUEUE_URL))
                .andExpect(status().isUnauthorized());
    }

    // MOD-TC-SEC-002: CONTENT_ADMIN cannot access moderation queue → 403 (TDS §16 Auth Matrix)
    @Test
    @WithMockUser(username = "1", roles = "CONTENT_ADMIN")
    void getQueue_asContentAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(QUEUE_URL))
                .andExpect(status().isForbidden());
    }

    // MOD-TC-SEC-002: EXPERT cannot access moderation queue → 403
    @Test
    @WithMockUser(username = "1", roles = "EXPERT")
    void getQueue_asExpertRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(QUEUE_URL))
                .andExpect(status().isForbidden());
    }

    // MOD-TC-SEC-003: SQL injection in targetType param → 400 (enum binding rejects it)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_sqlInjectionInTargetType_shouldReturn400() throws Exception {
        mockMvc.perform(get(QUEUE_URL + "?targetType=QUESTION'; DROP TABLE moderation_cases;--"))
                .andExpect(status().isBadRequest());
    }

    private static final String PENDING_CONTENT_URL = "/api/v1/admin/moderation/pending-content";
    private static final String HISTORY_URL = "/api/v1/admin/moderation/history";
    private static final String ACCOUNT_HISTORY_URL = "/api/v1/admin/moderation/account-history";
    private static final String ACCOUNT_HISTORY_DETAIL_URL = ACCOUNT_HISTORY_URL + "/"
            + java.util.UUID.fromString("44444444-0000-0000-0000-000000000001");

    // PCQ-TC-007: ROLE_MOTHER cannot access pending-content queue → 403 (same RBAC as /queue)
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getPendingContentQueue_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(PENDING_CONTENT_URL + "?targetType=QUESTION"))
                .andExpect(status().isForbidden());
    }

    // PCQH-TC-006: ROLE_MOTHER cannot access moderation history → 403 (same RBAC as /queue)
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getModerationHistory_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(HISTORY_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getAccountViolationHistory_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(ACCOUNT_HISTORY_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getAccountViolationDetail_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(ACCOUNT_HISTORY_DETAIL_URL))
                .andExpect(status().isForbidden());
    }

    private static final String CONTENT_DETAIL_URL = "/api/v1/admin/moderation/content/QUESTION/"
            + java.util.UUID.fromString("22222222-0000-0000-0000-000000000001");

    // DETAIL-TC-009: ROLE_MOTHER cannot access content detail → 403 (CWE-862)
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getContentDetail_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(CONTENT_DETAIL_URL))
                .andExpect(status().isForbidden());
    }

    // DETAIL-TC-010: no JWT → 401
    @Test
    void getContentDetail_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get(CONTENT_DETAIL_URL))
                .andExpect(status().isUnauthorized());
    }

    private static final String UNDO_URL = "/api/v1/admin/moderation/actions/"
            + java.util.UUID.fromString("33333333-0000-0000-0000-000000000001") + "/undo";

    // UNDO-TC-015: ROLE_MOTHER cannot undo a moderation action → 403 (CWE-862)
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void undoModerationAction_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(UNDO_URL)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    // UNDO-TC-016: no JWT → 401
    @Test
    void undoModerationAction_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(UNDO_URL))
                .andExpect(status().isUnauthorized());
    }

}
