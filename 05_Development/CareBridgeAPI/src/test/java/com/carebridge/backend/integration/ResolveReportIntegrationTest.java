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
import com.carebridge.backend.content.dto.response.ResolveReportResponse;
import com.carebridge.backend.content.entity.ModerationActionType;
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

// RES-TC-INT-001, RES-TC-INT-002: Full HTTP stack integration — Security -> Controller -> Service
// (mocked persistence). Note: no Testcontainers/real-DB harness exists in this codebase (verified,
// same finding as UC-100's ModerateContentIntegrationTest) — follows existing project convention.
// RES-TC-INT-003 (atomicity rollback) is covered at service-unit level in
// ResolveReportServiceImplTest#resolveReport_failureDuringActionSave_propagatesExceptionBeforeReportSave.
// RES-TC-INT-004 (race condition) requires real concurrent DB transactions and is explicitly flagged
// Open/best-effort by its own Test-Spec entry — not verifiable without a real-DB harness; not faked here.
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ResolveReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID REPORT_ID = UUID.fromString("ee000000-0000-0000-0000-000000000001");
    private static final UUID MODERATOR_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private static String resolveUrl() {
        return "/api/v1/admin/moderation/reports/" + REPORT_ID + "/resolve";
    }

    // RES-TC-INT-001: full API flow DISMISS -> 200 with reportStatus DISMISSED
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void resolveReport_dismiss_fullStackReturns200WithDismissedStatus() throws Exception {
        ResolveReportResponse response = new ResolveReportResponse(
                REPORT_ID, ReportStatus.DISMISSED, MODERATOR_ID, Instant.now(), null, null, null);
        when(moderationService.resolveReport(any(), any(), any())).thenReturn(response);

        String body = "{\"outcome\":\"DISMISS\",\"reason\":\"No violation found\"}";

        mockMvc.perform(post(resolveUrl()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(REPORT_ID.toString()))
                .andExpect(jsonPath("$.reportStatus").value("DISMISSED"))
                .andExpect(jsonPath("$.actionId").doesNotExist())
                .andExpect(jsonPath("$.actionType").doesNotExist());
    }

    // RES-TC-INT-002: full API flow HIDE -> 200 with reportStatus RESOLVED and action fields populated
    @Test
    @WithMockUser(username = "aaaaaaaa-0000-0000-0000-000000000001", roles = "MODERATOR")
    void resolveReport_hide_fullStackReturns200WithResolvedStatusAndAction() throws Exception {
        ResolveReportResponse response = new ResolveReportResponse(
                REPORT_ID, ReportStatus.RESOLVED, MODERATOR_ID, Instant.now(),
                UUID.randomUUID(), ModerationActionType.HIDE, "HIDDEN");
        when(moderationService.resolveReport(any(), any(), any())).thenReturn(response);

        String body = "{\"outcome\":\"HIDE\",\"reason\":\"Policy violation\"}";

        mockMvc.perform(post(resolveUrl()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.actionType").value("HIDE"))
                .andExpect(jsonPath("$.resultingStatus").value("HIDDEN"));
    }
}
