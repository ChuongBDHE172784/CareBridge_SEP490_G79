package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.service.ModerationService;
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

// RES-TC-123, RES-TC-124
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ResolveReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID REPORT_ID = UUID.fromString("ee000000-0000-0000-0000-000000000001");

    private static String resolveUrl() {
        return "/api/v1/admin/moderation/reports/" + REPORT_ID + "/resolve";
    }

    // RES-TC-123: missing required field (outcome=null) -> 400
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void resolveReport_missingOutcome_shouldReturn400() throws Exception {
        String bodyMissingOutcome = "{\"reason\":\"test\"}";

        mockMvc.perform(post(resolveUrl()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyMissingOutcome))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("outcome"));
    }

    // RES-TC-124: unexpected exception -> 500 INTERNAL_ERROR (not dead-code MOD-005)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void resolveReport_unexpectedException_shouldReturn500WithInternalError() throws Exception {
        when(moderationService.resolveReport(any(), any(), any()))
                .thenThrow(new RuntimeException("simulated failure"));

        String validBody = "{\"outcome\":\"DISMISS\"}";

        mockMvc.perform(post(resolveUrl()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
