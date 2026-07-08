package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.dto.response.UpdateContentResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.AdminContentService;
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

// UCT-TC-INT-001
// Note: no Testcontainers/real-DB harness exists in this codebase (verified, same finding as UC-100/101/102).
// This test exercises the full Spring MVC + Security filter chain against a mocked service layer,
// consistent with the established project convention (see ContentIntegrationTest, ModerateContentIntegrationTest).
@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class UpdateContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContentService adminContentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID C1 = UUID.fromString("f1500000-0000-0000-0000-000000000001");

    @Test
    @WithMockUser(username = "f1400000-0000-0000-0000-0000000000ad", roles = "CONTENT_ADMIN")
    void updateContent_fullStack_returns200WithIncrementedVersion() throws Exception {
        UpdateContentResponse response = new UpdateContentResponse(
                C1, ContentType.ARTICLE, "A updated", "updated body", ContentStage.PREGNANCY,
                null, ContentStatus.APPROVED, 3, Instant.now());
        when(adminContentService.updateContent(any(), any(), any())).thenReturn(response);

        String body = "{\"title\":\"A updated\",\"body\":\"updated body\",\"stage\":\"PREGNANCY\","
                + "\"status\":\"APPROVED\"}";

        mockMvc.perform(put("/api/v1/admin/content/" + C1).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.versionNo").value(3))
                .andExpect(jsonPath("$.data.type").value("ARTICLE"));
    }
}
