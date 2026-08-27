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
import com.carebridge.backend.content.dto.response.ClaimReportResponse;
import com.carebridge.backend.content.entity.ReportStatus;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** CB-MOD-IMP-016 scenario 19: claim/release endpoints are MODERATOR-only. */
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ClaimReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String MODERATOR_UUID = "33333333-3333-3333-3333-333333333333";
    private static final UUID REPORT_ID = UUID.randomUUID();

    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "MODERATOR")
    void claim_asModerator_returns200WithInReviewStatus() throws Exception {
        when(moderationService.claimReport(any(), any())).thenReturn(new ClaimReportResponse(
                REPORT_ID, ReportStatus.IN_REVIEW, UUID.fromString(MODERATOR_UUID), Instant.now()));
        mockMvc.perform(post("/api/v1/admin/moderation/reports/{id}/claim", REPORT_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "SYSTEM_ADMIN")
    void claim_asSystemAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/moderation/reports/{id}/claim", REPORT_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "MOTHER")
    void release_asMother_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/moderation/reports/{id}/release", REPORT_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void claim_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/moderation/reports/{id}/claim", REPORT_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
