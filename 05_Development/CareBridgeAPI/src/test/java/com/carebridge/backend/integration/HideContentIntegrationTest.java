package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.dto.response.HideContentResponse;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.service.AdminContentService;
import com.carebridge.backend.content.service.ContentService;
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

// UCT-TC-INT-1201, UCT-TC-1206 (integration-level portion)
// Note: no Testcontainers/real-DB harness exists in this codebase (verified, same finding as
// UC-100/101/102/106). This test exercises the full Spring MVC + Security filter chain against a mocked
// service layer. It verifies HTTP-level wiring/response shape only; it does NOT verify real DB row
// persistence or that a public read endpoint excludes the archived item end-to-end — the latter is a
// logical consequence of the existing status='APPROVED' filters already used by ContentRepository's
// findByFilters()/searchByFilters() queries (unchanged by this UC), not independently re-verified here.
// Row-not-deleted (UCT-TC-1206) is verified at the service-unit-test level instead (HideContentServiceImplTest).
@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class HideContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContentService adminContentService;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID D3 = UUID.fromString("f1700000-0000-0000-0000-000000000003");
    private static final UUID ADMIN_ID = UUID.fromString("f1700000-0000-0000-0000-0000000000ad");

    @Test
    @WithMockUser(username = "f1700000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void hideContent_fullStack_returns200WithArchivedStatus() throws Exception {
        HideContentResponse response = new HideContentResponse(
                D3, ContentStatus.APPROVED, ContentStatus.ARCHIVED, "Thông tin lỗi thời", ADMIN_ID, Instant.now());
        when(adminContentService.hideContent(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/content/" + D3 + "/archive").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Thông tin lỗi thời\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previousStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.newStatus").value("ARCHIVED"));
    }
}
