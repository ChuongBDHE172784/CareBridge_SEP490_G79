package com.carebridge.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.RedFlagRuleController;
import com.carebridge.backend.triage.service.RedFlagRuleService;
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

// RFR-TC-SEC-001, RFR-TC-SEC-002
@WebMvcTest(
        value = RedFlagRuleController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RedFlagRuleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedFlagRuleService redFlagRuleService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/red-flag-rules";
    private static final UUID RULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String CREATE_BODY =
            "{\"keyword\":\"tu khoa moi\",\"severity\":\"RED\",\"action\":\"ESCALATE\"}";

    // RFR-TC-SEC-001: MODERATOR rejected on every endpoint -> 403
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000bb", roles = "MODERATOR")
    void allEndpoints_asModerator_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(BASE_URL + "/" + RULE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + RULE_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(redFlagRuleService, never()).createRule(any(), any());
        verify(redFlagRuleService, never()).listRules(any());
        verify(redFlagRuleService, never()).updateRule(any(), any(), any());
        verify(redFlagRuleService, never()).deleteRule(any(), any());
    }

    // RFR-TC-SEC-002: missing JWT -> 401, empty body (verified real path: HttpStatusEntryPoint)
    @Test
    void list_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}
