package com.carebridge.backend.content.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.dto.request.ContentFilterRequest;
import com.carebridge.backend.content.dto.response.ChecklistItemResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateResponse;
import com.carebridge.backend.content.dto.response.ContentListResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentServiceImpl;
import com.carebridge.backend.content.support.Story69TestFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

// CNT82-TC-001, CNT82-TC-002, CNT82-TC-004
@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @Mock
    private LifecycleContentStageResolver lifecycleContentStageResolver;

    @Spy
    private ContentMapper contentMapper = new ContentMapper();

    @InjectMocks
    private ContentServiceImpl contentServiceImpl;

    // ── Props Isolation ──────────────────────────────────────────────────────
    private static final UUID CONTENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private ContentItem makeContentItem(ContentStatus status, ContentStage stage, ContentType type) {
        return ContentItem.builder()
                .id(CONTENT_ID)
                .type(type)
                .title("Test Content Title")
                .body("Test body content")
                .stage(stage)
                .status(status)
                .versionNo(1)
                .authorUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private ChecklistTemplate makeChecklistTemplate(ContentStage stage) {
        return ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .name("Test Checklist")
                .stage(stage)
                .description("Test description")
                .createdAt(Instant.now())
                .build();
    }

    private ChecklistItem makeChecklistItem(ChecklistTemplate template, int order) {
        return ChecklistItem.builder()
                .id(UUID.randomUUID())
                .template(template)
                .itemText("Item " + order)
                .order(order)
                .isRequired(order == 1)
                .createdAt(Instant.now())
                .build();
    }

    // ── CNT82-TC-001: Service luôn truyền APPROVED vào repository ────────────
    @Test
    void getContents_shouldAlwaysPassAPPROVEDStatusToRepository() {
        ContentItem approvedItem = makeContentItem(ContentStatus.APPROVED, ContentStage.PREGNANCY, ContentType.ARTICLE);
        Page<ContentItem> mockPage = new PageImpl<>(List.of(approvedItem));
        when(contentRepository.findByFilters(any(), eq(ContentStage.PREGNANCY), any(), eq(ContentStatus.APPROVED), any()))
                .thenReturn(mockPage);

        ContentFilterRequest filter = ContentFilterRequest.builder()
                .stage(ContentStage.PREGNANCY)
                .build();

        Page<ContentListResponse> result = contentServiceImpl.getContents(filter, PageRequest.of(0, 20));

        ArgumentCaptor<ContentStatus> statusCaptor = ArgumentCaptor.forClass(ContentStatus.class);
        verify(contentRepository).findByFilters(any(), any(), any(), statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(ContentStatus.APPROVED);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getContents_emptyResult_returnsEmptyPage() {
        when(contentRepository.findByFilters(any(), any(), any(), eq(ContentStatus.APPROVED), any()))
                .thenReturn(Page.empty());

        ContentFilterRequest filter = ContentFilterRequest.builder()
                .stage(ContentStage.PRE_PREGNANCY)
                .build();

        Page<ContentListResponse> result = contentServiceImpl.getContents(filter, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isZero();
    }

    // ── CNT82-TC-002: getContentById ném exception khi không tìm thấy ────────
    @Test
    void getContentById_shouldThrowCNT003WhenNotFound() {
        UUID randomId = UUID.randomUUID();
        when(contentRepository.findByIdAndStatus(eq(randomId), eq(ContentStatus.APPROVED)))
                .thenReturn(Optional.empty());

        ContentException ex = assertThrows(ContentException.class,
                () -> contentServiceImpl.getContentById(randomId));

        assertThat(ex.getCode()).isEqualTo("CNT-003");
        assertThat(ex.getHttpStatus().value()).isEqualTo(404);
    }

    @Test
    void getContentById_shouldThrowCNT003WhenContentIsDraft() {
        UUID draftId = UUID.randomUUID();
        // Repository returns empty because DRAFT doesn't match APPROVED filter (ADR-002)
        when(contentRepository.findByIdAndStatus(eq(draftId), eq(ContentStatus.APPROVED)))
                .thenReturn(Optional.empty());

        assertThrows(ContentException.class,
                () -> contentServiceImpl.getContentById(draftId));
    }

    // ── CNT82-TC-004: getChecklists trả về items đúng thứ tự ─────────────────
    @Test
    void getChecklists_shouldReturnItemsSortedByOrderAsc() {
        ChecklistTemplate template = makeChecklistTemplate(ContentStage.PREGNANCY);
        ChecklistItem item1 = makeChecklistItem(template, 1);
        ChecklistItem item2 = makeChecklistItem(template, 2);
        ChecklistItem item3 = makeChecklistItem(template, 3);

        when(checklistTemplateRepository.findByStageAndStatusOrderByUpdatedAtDesc(
                ContentStage.PREGNANCY, ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(template));
        // DB ORDER BY item_order returns already-sorted list
        when(checklistItemRepository.findAllByApprovedTemplateIds(
                java.util.Set.of(template.getId()), ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(item1, item2, item3));

        List<ChecklistTemplateResponse> result = contentServiceImpl.getChecklists(ContentStage.PREGNANCY);

        assertThat(result).hasSize(1);
        List<ChecklistItemResponse> items = result.get(0).getItems();
        assertThat(items).hasSize(3);
        assertThat(items.get(0).getOrder()).isEqualTo(1);
        assertThat(items.get(1).getOrder()).isEqualTo(2);
        assertThat(items.get(2).getOrder()).isEqualTo(3);
    }

    @Test
    void getChecklists_withNullStage_returnsAllTemplates() {
        ChecklistTemplate template = makeChecklistTemplate(ContentStage.PREGNANCY);
        when(checklistTemplateRepository.findByStatusOrderByUpdatedAtDesc(
                ChecklistTemplateStatus.APPROVED)).thenReturn(List.of(template));
        when(checklistItemRepository.findAllByApprovedTemplateIds(
                java.util.Set.of(template.getId()), ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of());

        List<ChecklistTemplateResponse> result = contentServiceImpl.getChecklists(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getChecklists_templateWithNoItems_returnsEmptyItemList() {
        ChecklistTemplate template = makeChecklistTemplate(ContentStage.POSTPARTUM);
        when(checklistTemplateRepository.findByStageAndStatusOrderByUpdatedAtDesc(
                ContentStage.POSTPARTUM, ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of(template));
        when(checklistItemRepository.findAllByApprovedTemplateIds(
                java.util.Set.of(template.getId()), ChecklistTemplateStatus.APPROVED))
                .thenReturn(List.of());

        List<ChecklistTemplateResponse> result = contentServiceImpl.getChecklists(ContentStage.POSTPARTUM);

        assertThat(result.get(0).getItems()).isNotNull().isEmpty();
    }

    @Test
    void uc82_69_tc_001_003_genericChecklistQueriesApprovedParentsAndBatchLoadsItems() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/content/service/ContentServiceImpl.java");

        assertThat(source.contains("ChecklistTemplateStatus.APPROVED"))
                .as("TC-001/003: generic checklists must query only approved template parents")
                .isTrue();
        assertThat(source.contains("findByStatusOrderByUpdatedAtDesc")
                        && source.contains("findByStageAndStatusOrderByUpdatedAtDesc"))
                .as("TC-001/003: both null-stage and explicit-stage paths include APPROVED")
                .isTrue();
        assertThat(source.contains("findAllByApprovedTemplateIds"))
                .as("TC-001: checklist items are loaded in one approved-parent batch")
                .isTrue();
        assertThat(source.contains("findByTemplate_IdOrderByOrder"))
                .as("TC-001: per-template N+1 item loading is forbidden")
                .isFalse();
    }

    @Test
    void uc82_69_tc_008_genericVerifiedBrowseRemainsApprovedAndIndependentOfLifecycle() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/content/service/ContentServiceImpl.java");
        assertThat(source.contains("getContents") && source.contains("searchContent")
                        && source.contains("ContentStatus.APPROVED"))
                .isTrue();
        assertThat(source.contains("filter.getStage()") && source.contains("request.getStage()"))
                .as("TC-008: deliberate generic browse retains its explicit stage filters")
                .isTrue();
    }

    @Test
    void uc82_69_tc_016_allLifecycleOperationsFailBeforeAnyDataRepositoryLookup() {
        UUID ownerId = UUID.fromString("69000000-0000-0000-0000-000000000301");
        UUID contentId = UUID.fromString("69000000-0000-0000-0000-000000000302");
        when(lifecycleContentStageResolver.resolve(ownerId))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        assertThatThrownBy(() -> contentServiceImpl.getLifecycleContents(
                        ownerId, ContentType.ARTICLE, null, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(ContentException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CNT-013"));
        assertThatThrownBy(() -> contentServiceImpl.getLifecycleChecklists(ownerId))
                .isInstanceOfSatisfying(ContentException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CNT-013"));
        assertThatThrownBy(() -> contentServiceImpl.getLifecycleContentById(ownerId, contentId))
                .isInstanceOfSatisfying(ContentException.class,
                        error -> assertThat(error.getCode()).isEqualTo("CNT-013"));

        verifyNoInteractions(contentRepository, checklistTemplateRepository, checklistItemRepository);
    }
}
