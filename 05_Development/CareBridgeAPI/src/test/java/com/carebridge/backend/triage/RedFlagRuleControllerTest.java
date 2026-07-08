package com.carebridge.backend.triage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.RedFlagRuleController;
import com.carebridge.backend.triage.service.RedFlagRuleService;
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

// RFR-TC-003
@WebMvcTest(
        value = RedFlagRuleController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RedFlagRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedFlagRuleService redFlagRuleService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String URL = "/api/v1/admin/red-flag-rules";

    // RFR-TC-003: blank keyword -> 400, service never invoked.
    // Real verified path: MethodArgumentNotValidException -> GlobalExceptionHandler generic
    // VALIDATION_ERROR envelope (same corrected finding pattern as this TDS's own IAM-001 fix) —
    // asserting status only, not a body error code, matching sibling AdminContentControllerTest.
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000aa", roles = "SYSTEM_ADMIN")
    void create_blankKeyword_shouldReturn400() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"\",\"severity\":\"RED\",\"action\":\"ESCALATE\"}"))
                .andExpect(status().isBadRequest());

        verify(redFlagRuleService, never()).createRule(any(), any());
    }
}
