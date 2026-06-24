package com.carebridge.backend.content.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.dto.request.ContentSearchRequest;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.content.service.ContentServiceImpl;
import java.time.Instant;
import java.util.List;
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

// CNT224-TC-001 through CNT224-TC-005 — Service unit tests
@ExtendWith(MockitoExtension.class)
class ContentSearchServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private ChecklistItemRepository checklistItemRepository;

    @Spy
    private ContentMapper contentMapper = new ContentMapper();

    @InjectMocks
    private ContentServiceImpl contentServiceImpl;

    // ── Props Isolation ──────────────────────────────────────────────────────
    private ContentItem makeApprovedContent(String title, ContentStage stage, ContentType type) {
        return ContentItem.builder()
                .id(UUID.randomUUID())
                .type(type)
                .title(title)
                .body("Test body for " + title)
                .stage(stage)
                .topicId(null)
                .status(ContentStatus.APPROVED)
                .versionNo(1)
                .authorUserId(1L)
                .publishedAt(Instant.now().minusSeconds(86400))
                .createdAt(Instant.now().minusSeconds(172800))
                .updatedAt(Instant.now().minusSeconds(86400))
                .build();
    }

    private ContentSearchRequest makeSearchRequest(String keyword) {
        ContentSearchRequest request = new ContentSearchRequest();
        request.setKeyword(keyword);
        return request;
    }

    // ── CNT224-TC-001: searchContent always passes APPROVED to repository ────
    // Oracle: BR-RBAC, ADR-003 — only APPROVED content visible
    @Test
    void searchContent_shouldAlwaysPassAPPROVEDStatusToRepository() {
        ContentItem approvedItem = makeApprovedContent("Thai ky tuan 12", ContentStage.PREGNANCY, ContentType.ARTICLE);
        Page<ContentItem> mockPage = new PageImpl<>(List.of(approvedItem));
        when(contentRepository.searchByFilters(
                eq("thai ky"), any(), any(), any(), eq(ContentStatus.APPROVED), any()))
                .thenReturn(mockPage);

        ContentSearchRequest request = makeSearchRequest("thai ky");

        Page<ContentSearchResponse> result = contentServiceImpl.searchContent(request, PageRequest.of(0, 20));

        ArgumentCaptor<ContentStatus> statusCaptor = ArgumentCaptor.forClass(ContentStatus.class);
        verify(contentRepository).searchByFilters(any(), any(), any(), any(), statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(ContentStatus.APPROVED);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── CNT224-TC-002: Keyword is trimmed before passing to repository ───────
    // Oracle: ADR-004 — keyword trimmed before query
    @Test
    void sanitizeKeyword_shouldTrimLeadingAndTrailingSpaces() {
        String rawKeyword = "  thai ky  "; // FX-224-006
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(contentRepository.searchByFilters(keywordCaptor.capture(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        contentServiceImpl.searchContent(makeSearchRequest(rawKeyword), PageRequest.of(0, 20));

        assertThat(keywordCaptor.getValue()).isEqualTo("thai ky");
        assertThat(keywordCaptor.getValue()).doesNotStartWith(" ");
        assertThat(keywordCaptor.getValue()).doesNotEndWith(" ");
    }

    // ── CNT224-TC-003: LIKE wildcard characters are escaped ──────────────────
    // Oracle: ADR-004 — escape % and _ to prevent unintended LIKE matches
    @Test
    void sanitizeKeyword_shouldEscapePercentWildcard() {
        String rawKeyword = "100% safe"; // FX-224-007
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(contentRepository.searchByFilters(keywordCaptor.capture(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        contentServiceImpl.searchContent(makeSearchRequest(rawKeyword), PageRequest.of(0, 20));

        String sanitized = keywordCaptor.getValue();
        assertThat(sanitized).contains("\\%");
        assertThat(sanitized).doesNotContain("100% ");
    }

    @Test
    void sanitizeKeyword_shouldEscapeUnderscoreWildcard() {
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(contentRepository.searchByFilters(keywordCaptor.capture(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        contentServiceImpl.searchContent(makeSearchRequest("thai_ky"), PageRequest.of(0, 20));

        assertThat(keywordCaptor.getValue()).contains("\\_");
    }

    // ── CNT224-TC-005: topicName null-safe when topicId is null ─────────────
    // Oracle: CB-CONTENT-IMP-002 §8.1 — topicName resolved; null if topicId null
    @Test
    void searchContent_contentWithNullTopicId_shouldReturnNullTopicName() {
        ContentItem itemWithNullTopic = makeApprovedContent("Test", ContentStage.PREGNANCY, ContentType.ARTICLE);
        Page<ContentItem> mockPage = new PageImpl<>(List.of(itemWithNullTopic));
        when(contentRepository.searchByFilters(any(), any(), any(), any(), eq(ContentStatus.APPROVED), any()))
                .thenReturn(mockPage);

        Page<ContentSearchResponse> result = contentServiceImpl.searchContent(
                makeSearchRequest("test"), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        // No NullPointerException; topicName is null when topicId is null
        assertThat(result.getContent().get(0).getTopicName()).isNull();
    }
}
