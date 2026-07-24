package com.carebridge.backend.content.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.dto.response.ContentDetailResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.content.support.Story69TestFactory;
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
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

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

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContentById_uc225_responseDoesNotExposeAuthorIdentity() throws Exception {
        UUID id = UUID.randomUUID();
        when(contentService.getContentById(id)).thenReturn(ContentDetailResponse.builder()
                .id(id).type(ContentType.ARTICLE).title("Verified").body("Body")
                .stage(ContentStage.PREGNANCY).version(1).build());

        String json = mockMvc.perform(get("/api/v1/content/" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("authorId").doesNotContain("authorUserId");
    }

    @Test
    void getContentById_uc225_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/content/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
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

    @Test
    void uc82_69_tc_017_noConsumerTemplateDetailEndpointIsIntroduced() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/content/controller/ContentController.java");
        assertThat(source.contains("@GetMapping(\"/checklists/{templateId}\")")
                        || source.contains("@GetMapping(\"/checklists/{id}\")"))
                .as("TC-017: consumer template detail must remain unmapped")
                .isFalse();
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void uc82_69_tc_017_templateDetailRequestUsesFrameworkNotFoundWithoutTemplateData()
            throws Exception {
        UUID templateId = UUID.fromString("69000000-0000-0000-0000-000000000401");
        String body = mockMvc.perform(get("/api/v1/content/checklists/" + templateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("Synthetic private template", "itemText", "description",
                        "NoResourceFoundException", "No static resource", "INTERNAL_ERROR");
        verifyNoInteractions(contentService);
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void unmatchedApiRouteReturnsTheSameNeutral404WithoutInternalResourceMetadata()
            throws Exception {
        String body = mockMvc.perform(get("/api/v1/unmatched-story-69-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("NoResourceFoundException", "No static resource", "INTERNAL_ERROR");
        verifyNoInteractions(contentService);
    }
}
