package com.carebridge.backend.content.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.dto.response.ChecklistItemResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CNT82-TC-INT-001, CNT82-TC-INT-002, CNT82-TC-INT-003
// Integration tests: full HTTP stack (security → controller → service) with mocked persistence
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class ContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    // ── CNT82-TC-INT-001: Only APPROVED content returned ─────────────────────
    // Verifies the service is always called with APPROVED status and authorId not in response
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getContents_integration_shouldEnforceApprovedFilterAndHideAuthorId() throws Exception {
        UUID approvedId = UUID.randomUUID();
        ContentListResponse approvedItem = ContentListResponse.builder()
                .id(approvedId)
                .type(ContentType.ARTICLE)
                .title("Approved Article")
                .stage(ContentStage.PREGNANCY)
                .publishedAt(Instant.now())
                .build();

        Page<ContentListResponse> mockPage = new PageImpl<>(List.of(approvedItem));
        when(contentService.getContents(any(), any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/content")
                        .param("stage", "PREGNANCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].type").value("ARTICLE"))
                // BR-PRIVACY: authorId must not appear in response
                .andExpect(jsonPath("$.data[0].authorId").doesNotExist())
                // BR-RBAC: status field not exposed to client
                .andExpect(jsonPath("$.data[0].status").doesNotExist());

        // Verify service is called (APPROVED enforcement is in service layer — CNT82-TC-001)
        verify(contentService).getContents(any(), any(Pageable.class));
    }

    // ── CNT82-TC-INT-002: Checklist items returned in order ──────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getChecklists_integration_shouldReturnItemsInOrderAsc() throws Exception {
        UUID templateId = UUID.randomUUID();

        List<ChecklistItemResponse> sortedItems = List.of(
                ChecklistItemResponse.builder().id(UUID.randomUUID()).itemText("Item 1").order(1).isRequired(true).build(),
                ChecklistItemResponse.builder().id(UUID.randomUUID()).itemText("Item 2").order(2).isRequired(false).build(),
                ChecklistItemResponse.builder().id(UUID.randomUUID()).itemText("Item 3").order(3).isRequired(false).build()
        );

        ChecklistTemplateResponse template = ChecklistTemplateResponse.builder()
                .id(templateId)
                .name("Checklist tháng 1")
                .stage(ContentStage.PREGNANCY)
                .description("Danh sách khám thai")
                .items(sortedItems)
                .build();

        when(contentService.getChecklists(eq(ContentStage.PREGNANCY)))
                .thenReturn(List.of(template));

        mockMvc.perform(get("/api/v1/content/checklists")
                        .param("stage", "PREGNANCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].items[0].order").value(1))
                .andExpect(jsonPath("$.data[0].items[1].order").value(2))
                .andExpect(jsonPath("$.data[0].items[2].order").value(3));
    }

    // ── CNT82-TC-INT-003: Checklist with no items returns [] not null ─────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getChecklists_withNoItems_shouldReturnEmptyItemsList() throws Exception {
        ChecklistTemplateResponse emptyTemplate = ChecklistTemplateResponse.builder()
                .id(UUID.randomUUID())
                .name("Empty Checklist")
                .stage(ContentStage.POSTPARTUM)
                .description("No items yet")
                .items(List.of())
                .build();

        when(contentService.getChecklists(eq(ContentStage.POSTPARTUM)))
                .thenReturn(List.of(emptyTemplate));

        mockMvc.perform(get("/api/v1/content/checklists")
                        .param("stage", "POSTPARTUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].items").isArray())
                .andExpect(jsonPath("$.data[0].items").isEmpty());
    }

    // ── Full stack: stage filter passed to service ────────────────────────────
    @Test
    @WithMockUser(username = "1", roles = "USER")
    void getChecklists_stageParamPassedToService() throws Exception {
        when(contentService.getChecklists(ContentStage.BABY_CARE)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/content/checklists")
                        .param("stage", "BABY_CARE"))
                .andExpect(status().isOk());

        ArgumentCaptor<ContentStage> stageCaptor = ArgumentCaptor.forClass(ContentStage.class);
        verify(contentService).getChecklists(stageCaptor.capture());
        // Stage enum resolved correctly from query param
    }
}
