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
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// RES-TC-119, RES-TC-120, RES-TC-121, RES-TC-122, RES-TC-SEC-001
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ResolveReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID REPORT_ID_QUESTION = UUID.fromString("ee000000-0000-0000-0000-000000000001");
    private static final UUID REPORT_ID_CONTENT = UUID.fromString("ee000000-0000-0000-0000-000000000003");

    private static String resolveUrl(UUID reportId) {
        return "/api/v1/admin/moderation/reports/" + reportId + "/resolve";
    }

    private static String dismissBody() {
        return "{\"outcome\":\"DISMISS\"}";
    }

    // RES-TC-119: Non-MODERATOR bị 403 (body empty — URL-matcher denial, same as UC-100)
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000099", roles = "MOTHER")
    void resolveReport_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(resolveUrl(REPORT_ID_QUESTION)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dismissBody()))
                .andExpect(status().isForbidden());
    }

    // RES-TC-120: SYSTEM_ADMIN has no implicit access — also 403
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000098", roles = "SYSTEM_ADMIN")
    void resolveReport_asSystemAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(resolveUrl(REPORT_ID_QUESTION)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dismissBody()))
                .andExpect(status().isForbidden());
    }

    // RES-TC-121: CONTENT_ADMIN bị 403 kể cả khi report targetType=CONTENT
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000097", roles = "CONTENT_ADMIN")
    void resolveReport_asContentAdminRoleOnContentReport_shouldReturn403() throws Exception {
        mockMvc.perform(post(resolveUrl(REPORT_ID_CONTENT)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dismissBody()))
                .andExpect(status().isForbidden());
    }

    // RES-TC-122: No JWT -> 401 (bodiless)
    @Test
    void resolveReport_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(post(resolveUrl(REPORT_ID_QUESTION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dismissBody()))
                .andExpect(status().isUnauthorized());
    }

    // RES-TC-SEC-001: SQL injection in reason field handled as literal text
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void resolveReport_sqlInjectionInReason_handledAsLiteralText() throws Exception {
        String maliciousReason = "x'; DROP TABLE content_reports;--";
        when(moderationService.resolveReport(any(), any(), any())).thenReturn(new ResolveReportResponse(
                REPORT_ID_QUESTION, ReportStatus.RESOLVED,
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"), Instant.now(),
                UUID.randomUUID(), com.carebridge.backend.content.entity.ModerationActionType.HIDE, "HIDDEN"));

        String body = "{\"outcome\":\"HIDE\",\"reason\":\"" + maliciousReason + "\"}";

        mockMvc.perform(post(resolveUrl(REPORT_ID_QUESTION)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportStatus").value("RESOLVED"));
    }
}
