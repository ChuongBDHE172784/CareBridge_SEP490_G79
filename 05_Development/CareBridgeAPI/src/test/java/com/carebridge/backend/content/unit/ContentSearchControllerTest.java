package com.carebridge.backend.content.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CNT224-TC-004, CNT224-TC-006, CNT224-TC-007, CNT224-TC-008 — Controller validation tests
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class ContentSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── CNT224-TC-004: Empty results → 200 with content:[], NOT 404 ──────────
    // Oracle: CB-CONTENT-IMP-002 §9.2 — empty search returns HTTP 200
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withNoResults_shouldReturn200NotEmpty() throws Exception {
        when(contentService.searchContent(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "nonexistentterm123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── CNT224-TC-006: keyword required — empty/blank/missing → 400 ──────────
    // Oracle: CB-CONTENT-IMP-002 §9.2, ADR-004
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withEmptyKeyword_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withBlankKeyword_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withoutKeywordParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/content/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
    }

    // ── CNT224-TC-007: keyword boundary — 100 chars valid, 101 chars invalid ──
    // Oracle: ADR-004, CB-CONTENT-IMP-002 §9.2
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withKeyword100Chars_shouldReturn200() throws Exception {
        String keyword100 = "a".repeat(100);
        when(contentService.searchContent(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", keyword100))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withKeyword101Chars_shouldReturn400() throws Exception {
        String keyword101 = "a".repeat(101);

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", keyword101))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
    }

    // ── CNT224-TC-008: size boundary — ≤50 valid, >50 invalid ───────────────
    // Oracle: CB-CONTENT-IMP-002 §9.2
    @ParameterizedTest
    @ValueSource(ints = {1, 20, 50})
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withValidSize_shouldReturn200(int size) throws Exception {
        when(contentService.searchContent(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "test")
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(ints = {51, 100, 200})
    @WithMockUser(username = "1", roles = "USER")
    void searchContent_withSizeOver50_shouldReturn400(int size) throws Exception {
        mockMvc.perform(get("/api/v1/content/search")
                        .param("keyword", "test")
                        .param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
    }
}
