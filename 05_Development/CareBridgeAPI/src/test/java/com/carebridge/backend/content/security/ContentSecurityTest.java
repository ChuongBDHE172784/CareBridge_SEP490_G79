package com.carebridge.backend.content.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import java.util.UUID;
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

// CNT82-TC-SEC-001, CNT82-TC-SEC-002
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class ContentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ── CNT82-TC-SEC-001: Unauthenticated requests → 401 ─────────────────────
    // Oracle: BR-RBAC — all /api/v1/** require JWT (SecurityConfig)
    // Note: HttpStatusEntryPoint returns 401 with empty body (no JSON body check)
    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/content",
        "/api/v1/content/checklists"
    })
    void allContentEndpoints_shouldReturn401WithoutJwt(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getContentById_unauthenticated_shouldReturn401() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/content/" + randomId))
                .andExpect(status().isUnauthorized());
    }

    // ── CNT82-TC-SEC-002: DRAFT content inaccessible via GET /{id} ───────────
    // Oracle: ADR-002 — repository filters status=APPROVED; DRAFT returns Optional.empty → 404
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContentById_withDraftId_shouldReturn404() throws Exception {
        UUID draftId = UUID.randomUUID();
        when(contentService.getContentById(draftId))
                .thenThrow(ContentException.contentNotFound());

        mockMvc.perform(get("/api/v1/content/" + draftId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CNT-003"))
                .andExpect(jsonPath("$.message").value("Content not found or not available"));
    }

    // ── BR-RBAC: authenticated user can access content ────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContents_authenticatedUser_shouldReturn200() throws Exception {
        when(contentService.getContents(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/content"))
                .andExpect(status().isOk());
    }
}
