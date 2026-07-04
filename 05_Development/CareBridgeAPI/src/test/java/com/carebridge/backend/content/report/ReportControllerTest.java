package com.carebridge.backend.content.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ReportController;
import com.carebridge.backend.content.dto.response.CreateReportResponse;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.service.ReportService;
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

/** RPT-TC-014-005..007 (Test-Spec §4) — controller validation/auth (no business logic here). */
@WebMvcTest(
        value = ReportController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ReportControllerTest {

    private static final String BASE_URL = "/api/v1/reports";
    private static final String USER_ID = "00000000-0000-0000-0000-000000000014";
    private static final String TARGET_ID = "bbbbbbbb-0000-0000-0000-000000000014";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReportService reportService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private String validBody() {
        return """
                {
                  "targetType": "QUESTION",
                  "targetId": "%s",
                  "category": "HARASSMENT",
                  "description": "Nội dung xúc phạm"
                }
                """.formatted(TARGET_ID);
    }

    // No JWT → 401 (CWE-306)
    @Test
    void createReport_noJwt_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());

        verify(reportService, never()).createReport(any(), any());
    }

    // Missing targetType → 400
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void createReport_missingTargetType_returns400() throws Exception {
        String body = """
                {"targetId": "%s", "category": "HARASSMENT"}
                """.formatted(TARGET_ID);

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createReport(any(), any());
    }

    // description > 500 chars → 400
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void createReport_descriptionTooLong_returns400() throws Exception {
        String longDescription = "A".repeat(501);
        String body = """
                {"targetType":"QUESTION","targetId":"%s","category":"SPAM","description":"%s"}
                """.formatted(TARGET_ID, longDescription);

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(reportService, never()).createReport(any(), any());
    }

    // Happy path: valid request → 201
    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void createReport_validRequest_returns201() throws Exception {
        CreateReportResponse response = CreateReportResponse.builder()
                .reportId(UUID.randomUUID())
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        when(reportService.createReport(any(), eq(UUID.fromString(USER_ID)))).thenReturn(response);

        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reportId").isNotEmpty());
    }
}
