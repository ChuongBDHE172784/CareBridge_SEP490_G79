package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentApprovalController;
import com.carebridge.backend.content.dto.response.ContentDecisionResponse;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.service.ContentApprovalService;
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

// CAV-TC-INT-001
// Note: no Testcontainers/real-DB harness exists in this codebase (verified, same finding as
// UC-100/101/102/106/107). This test exercises the full Spring MVC + Security filter chain against a
// mocked service layer. It verifies HTTP-level wiring/response shape (status change PENDING_REVIEW ->
// APPROVED reflected in the response) only; it does NOT verify real DB row persistence or independently
// re-run the public-read-path visibility query end to end — that visibility guarantee is a logical
// consequence of the existing status='APPROVED' filters already used by ContentRepository's
// findByFilters()/searchByFilters() queries (unchanged by this UC, and PENDING_REVIEW is a new enum value
// that those filters never match), not independently re-verified here.
@WebMvcTest(
        value = ContentApprovalController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ContentApprovalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentApprovalService contentApprovalService;

    @MockitoBean
    private com.carebridge.backend.content.service.ExpertContentApprovalService expertContentApprovalService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID CONTENT_ID = UUID.fromString("f1700000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("f1600000-0000-0000-0000-0000000000ad");

    @Test
    @WithMockUser(username = "f1600000-0000-0000-0000-0000000000ad", roles = "SYSTEM_ADMIN")
    void decide_approve_fullStack_returns200WithApprovedStatus() throws Exception {
        ContentDecisionResponse response = new ContentDecisionResponse(
                CONTENT_ID, ContentStatus.PENDING_REVIEW, ContentStatus.APPROVED, 3, ADMIN_ID, null, Instant.now());
        when(contentApprovalService.decide(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/content/" + CONTENT_ID + "/decision").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previousStatus").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.newStatus").value("APPROVED"));
    }
}
