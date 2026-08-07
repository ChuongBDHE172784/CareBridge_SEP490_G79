package com.carebridge.backend.aimoderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.aimoderation.controller.AiModerationAdminController;
import com.carebridge.backend.aimoderation.dto.response.AiModerationStatusResponse;
import com.carebridge.backend.aimoderation.dto.response.AiPolicyPageResponse;
import com.carebridge.backend.aimoderation.service.AiModerationStatusService;
import com.carebridge.backend.aimoderation.service.AiPolicyService;
import com.carebridge.backend.aimoderation.service.AiScanEnqueueService;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers;

/** Scenario 19 (RBAC) + scenario 7 (status endpoint never exposes the key). */
@WebMvcTest(
        value = AiModerationAdminController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AiModerationAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiPolicyService policyService;

    @MockitoBean
    private AiModerationStatusService statusService;

    @MockitoBean
    private AiScanEnqueueService enqueueService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String POLICIES_URL = "/api/v1/admin/ai-moderation/policies";
    private static final String STATUS_URL = "/api/v1/admin/ai-moderation/status";

    // SYSTEM_ADMIN manages AI policies
    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "SYSTEM_ADMIN")
    void listPolicies_asSystemAdmin_returns200() throws Exception {
        when(policyService.listPolicies(any(), anyInt(), anyInt()))
                .thenReturn(new AiPolicyPageResponse(List.of(), 0, 0, 50));
        mockMvc.perform(get(POLICIES_URL)).andExpect(status().isOk());
    }

    // Policy authoring is SYSTEM_ADMIN-only: no other role may even read the policy catalogue.
    @ParameterizedTest
    @ValueSource(strings = {"MODERATOR", "CONTENT_ADMIN", "EXPERT", "MOTHER"})
    void listPolicies_asNonSystemAdmin_returns403(String role) throws Exception {
        mockMvc.perform(get(POLICIES_URL)
                        .with(user("11111111-1111-1111-1111-111111111111").roles(role)))
                .andExpect(status().isForbidden());

        verify(policyService, never()).listPolicies(any(), anyInt(), anyInt());
    }

    // The one deliberate exception to the class-level rule: the moderator pending-content queue
    // reads /status to know whether AI screening is live. It is read-only and carries no policy
    // content, so MODERATOR is admitted here — and nowhere else on this controller.
    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "MODERATOR")
    void status_asModerator_returns200() throws Exception {
        when(statusService.status()).thenReturn(new AiModerationStatusResponse(
                true, true, "gemini-1.5-flash", "READY", true, 3, 1, 0, Instant.now(), "hash", 11));

        mockMvc.perform(get(STATUS_URL)).andExpect(status().isOk());
    }

    // CONTENT_ADMIN has no moderator queue, so it does not get the /status exception either.
    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "CONTENT_ADMIN")
    void status_asContentAdmin_returns403() throws Exception {
        mockMvc.perform(get(STATUS_URL)).andExpect(status().isForbidden());
    }

    // Body is fully valid on purpose: @Valid binding runs before method security, so an
    // invalid body would 400 first and mask the authorization check we want to exercise.
    private static final String VALID_POLICY_BODY = """
            {"policyCode":"NEW_POLICY","name":"n","detectionGuidance":"g",
             "violationCategory":"OTHER","reportCategory":"OTHER","severity":"LOW",
             "applicableTargetTypes":["QUESTION"],"confidenceThreshold":0.7,"active":true}
            """;

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "MOTHER")
    void createPolicy_asMother_returns403() throws Exception {
        mockMvc.perform(post(POLICIES_URL).with(csrf())
                        .contentType("application/json")
                        .content(VALID_POLICY_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void status_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(STATUS_URL)).andExpect(status().isUnauthorized());
    }

    // Scenario 7: the status payload exposes state only — never any API key material
    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "SYSTEM_ADMIN")
    void status_neverExposesApiKey() throws Exception {
        when(statusService.status()).thenReturn(new AiModerationStatusResponse(
                true, true, "gemini-1.5-flash", "READY", true, 3, 1, 0, Instant.now(), "hash", 11));
        mockMvc.perform(get(STATUS_URL))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("apiKey"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("api-key"))));
    }
}
