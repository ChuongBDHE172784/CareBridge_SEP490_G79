package com.carebridge.backend.content.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
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

// CNT82-TC-005, plus happy-path controller tests
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private ContentListResponse makeListItem(UUID id) {
        return ContentListResponse.builder()
                .id(id)
                .type(ContentType.ARTICLE)
                .title("Chăm sóc thai kỳ tuần 12")
                .stage(ContentStage.PREGNANCY)
                .publishedAt(Instant.now())
                .build();
    }

    // ── CNT82-TC-005a: size=100 → 400 CNT-001 ─────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContents_shouldRejectSizeGreaterThan50() throws Exception {
        mockMvc.perform(get("/api/v1/content")
                        .param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
    }

    // ── CNT82-TC-005b: size=50 → 200 ──────────────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContents_shouldAcceptSize50() throws Exception {
        when(contentService.getContents(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/content")
                        .param("size", "50"))
                .andExpect(status().isOk());
    }

    // ── Happy path: list ───────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContents_happyPath_shouldReturn200WithPaginatedResponse() throws Exception {
        UUID itemId = UUID.randomUUID();
        Page<ContentListResponse> mockPage = new PageImpl<>(List.of(makeListItem(itemId)));
        when(contentService.getContents(any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/content")
                        .param("stage", "PREGNANCY")
                        .param("type", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("ARTICLE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── Happy path: detail ─────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContentById_happyPath_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        ContentDetailResponse detail = ContentDetailResponse.builder()
                .id(id)
                .type(ContentType.ARTICLE)
                .title("Detail Title")
                .body("<p>Body</p>")
                .stage(ContentStage.PREGNANCY)
                .version(1)
                .publishedAt(Instant.now())
                .build();
        when(contentService.getContentById(id)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/content/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Detail Title"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    // ── 404 for non-existent or DRAFT content ─────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContentById_notFound_shouldReturn404WithCNT003() throws Exception {
        UUID randomId = UUID.randomUUID();
        when(contentService.getContentById(randomId))
                .thenThrow(ContentException.contentNotFound());

        mockMvc.perform(get("/api/v1/content/" + randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CNT-003"));
    }

    // ── Happy path: checklists ─────────────────────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getChecklists_happyPath_shouldReturn200() throws Exception {
        when(contentService.getChecklists(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/content/checklists")
                        .param("stage", "PREGNANCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── CONTENT_ADMIN role allowed ─────────────────────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "CONTENT_ADMIN")
    void getContents_contentAdminRole_shouldReturn200() throws Exception {
        when(contentService.getContents(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/content"))
                .andExpect(status().isOk());
    }
}
