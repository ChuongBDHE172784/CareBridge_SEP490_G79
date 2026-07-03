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
        mockMvc.perform(get(QUEUE_URL + "?targetType=QUESTION'; DROP TABLE content_reports;--"))
                .andExpect(status().isBadRequest());
    }

    private static final String PENDING_CONTENT_URL = "/api/v1/admin/moderation/pending-content";

    // PCQ-TC-007: ROLE_MOTHER cannot access pending-content queue → 403 (same RBAC as /queue)
    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getPendingContentQueue_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(PENDING_CONTENT_URL + "?targetType=QUESTION"))
                .andExpect(status().isForbidden());
    }
}
