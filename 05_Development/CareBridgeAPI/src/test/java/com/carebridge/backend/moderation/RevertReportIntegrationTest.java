package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.RevertReportResponse;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
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

// CB-MOD-IMP-015 — MRR-TC-INT-001/MRR-TC-016: full HTTP stack (Security -> Controller -> Service,
// mocked persistence). Follows this package's existing "integration test" convention
// (WebMvcTest + mocked service — see UndoModerationActionIntegrationTest.java); the module has no
// Testcontainers harness (verified project-wide), so real DB-state assertions for the happy path and
// guard failures already live at the unit level (ModerationServiceImplTest MRR-TC-001..012).
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RevertReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID REPORT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final String REVERT_URL = "/api/v1/admin/moderation/reports/" + REPORT_ID + "/revert";

    // MRR-TC-INT-001 (happy path leg): revert succeeds over the full HTTP stack, reportStatus=PENDING
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void revertReport_happyPath_returns201WithPendingStatus() throws Exception {
        RevertReportResponse response = new RevertReportResponse(
                REPORT_ID, ReportStatus.PENDING, UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), ReportTargetType.ANSWER, UUID.randomUUID(), "PENDING");
        when(moderationService.revertReport(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post(REVERT_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"bấm nhầm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportStatus").value("PENDING"))
                .andExpect(jsonPath("$.reportId").value(REPORT_ID.toString()));
    }

    // MRR-TC-INT-001 (no-body leg): request body is optional (RevertReportRequest.reason nullable)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void revertReport_withoutRequestBody_returns201() throws Exception {
        RevertReportResponse response = new RevertReportResponse(
                REPORT_ID, ReportStatus.PENDING, UUID.randomUUID(), Instant.now(), null, null, null, null);
        when(moderationService.revertReport(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post(REVERT_URL).with(csrf()))
                .andExpect(status().isCreated());
    }

    // MRR-TC-016: calling revert a second time on an already-PENDING report fails BR-MOD-015 -> 400 MOD-032
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void revertReport_calledTwiceOnSameReportId_secondCallReturns400() throws Exception {
        when(moderationService.revertReport(any(), any(), any()))
                .thenThrow(ModerationException.reportNotYetResolved(REPORT_ID));

        mockMvc.perform(post(REVERT_URL).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-032"));
    }
}
