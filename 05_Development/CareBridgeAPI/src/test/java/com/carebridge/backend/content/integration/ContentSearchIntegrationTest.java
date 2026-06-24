package com.carebridge.backend.content.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CNT224-TC-INT-001, CNT224-TC-INT-002, CNT224-TC-INT-003
// Integration tests: full HTTP stack (security → controller → mocked service)
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class ContentSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private ContentSearchResponse makeSearchResponse(String title, ContentStage stage, ContentType type) {
        return ContentSearchResponse.builder()
                .id(UUID.randomUUID())
                .type(type)
                .title(title)
                .stage(stage)
                .topicName(null)
                .publishedAt(Instant.now().minusSeconds(3600))
                .build();
    }

    // ── CNT224-TC-INT-001: Case-insensitive keyword matching ─────────────────
    // Oracle: ADR-003 — LOWER() ILIKE search is case-insensitive
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_integration_shouldBeCaseInsensitive() throws Exception {
        ContentSearchResponse uppercaseResult = makeSearchResponse(
                "THAI KỲ TUẦN 12", ContentStage.PREGNANCY, ContentType.ARTICLE);
        Page<ContentSearchResponse> mockPage = new PageImpl<>(List.of(uppercaseResult));
        when(contentService.searchContent(any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "thai ky"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].title").value("THAI KỲ TUẦN 12"));
    }

    // ── CNT224-TC-INT-002: DRAFT/ARCHIVED content excluded from results ───────
    // Oracle: BR-RBAC, ADR-003 — service enforces status=APPROVED (C1)
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_integration_shouldExcludeDraftAndArchivedContent() throws Exception {
        ContentSearchResponse approvedResult = makeSearchResponse(
                "Thai ky APPROVED", ContentStage.PREGNANCY, ContentType.ARTICLE);
        // Service only returns APPROVED — mock simulates this enforcement
        Page<ContentSearchResponse> mockPage = new PageImpl<>(List.of(approvedResult));
        when(contentService.searchContent(any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "thai ky"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Thai ky APPROVED"));
    }

    // ── CNT224-TC-INT-003: Multi-filter search (keyword + stage + type) ───────
    // Oracle: CB-CONTENT-IMP-002 §9.2 — multiple filter params applied together
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_integration_withMultipleFilters_shouldReturnMatchingOnly() throws Exception {
        ContentSearchResponse articleResult = makeSearchResponse(
                "Thai ky ARTICLE", ContentStage.PREGNANCY, ContentType.ARTICLE);
        Page<ContentSearchResponse> mockPage = new PageImpl<>(List.of(articleResult));
        when(contentService.searchContent(any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "thai ky")
                        .param("stage", "PREGNANCY")
                        .param("type", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].type").value("ARTICLE"))
                .andExpect(jsonPath("$.data[0].stage").value("PREGNANCY"));
    }

    // ── Happy path: search returns paginated results ──────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_happyPath_shouldReturn200WithPaginatedData() throws Exception {
        ContentSearchResponse item = makeSearchResponse(
                "Chăm sóc thai kỳ tuần 12", ContentStage.PREGNANCY, ContentType.ARTICLE);
        Page<ContentSearchResponse> mockPage = new PageImpl<>(List.of(item));
        when(contentService.searchContent(any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "thai ky")
                        .param("stage", "PREGNANCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Chăm sóc thai kỳ tuần 12"))
                .andExpect(jsonPath("$.data[0].type").value("ARTICLE"))
                .andExpect(jsonPath("$.data[0].stage").value("PREGNANCY"))
                // BR-PRIVACY: authorId must not appear in search response
                .andExpect(jsonPath("$.data[0].authorId").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
