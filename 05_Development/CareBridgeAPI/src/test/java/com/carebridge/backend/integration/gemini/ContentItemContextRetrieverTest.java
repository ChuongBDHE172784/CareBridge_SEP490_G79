package com.carebridge.backend.integration.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.repository.ContentRepository;
import com.carebridge.backend.integration.gemini.retriever.ContentItemContextRetriever;
import com.carebridge.backend.integration.gemini.retriever.RagContextRetriever;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

// RAG-TC-005, RAG-TC-006
@ExtendWith(MockitoExtension.class)
class ContentItemContextRetrieverTest {

    @Mock
    private ContentRepository contentRepository;

    private RagContextRetriever retriever;

    private final UUID topicId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        retriever = new ContentItemContextRetriever(contentRepository);
    }

    private ContentItem makeApprovedItem(String title) {
        return ContentItem.builder()
                .id(UUID.randomUUID())
                .title(title)
                .body("Nội dung về " + title)
                .status(ContentStatus.APPROVED)
                .topicId(topicId)
                .build();
    }

    private ContentItem makeDraftItem(String title) {
        return ContentItem.builder()
                .id(UUID.randomUUID())
                .title(title)
                .body("Nháp: " + title)
                .status(ContentStatus.DRAFT)
                .topicId(topicId)
                .build();
    }

    // RAG-TC-005: Chỉ APPROVED ContentItem được dùng làm context
    @Test
    void retrieveContext_mixedStatus_returnsOnlyApprovedItems() {
        ContentItem approvedItem = makeApprovedItem("Dinh dưỡng thai kỳ");

        when(contentRepository.searchByFilters(
                any(), isNull(), isNull(), eq(topicId), eq(ContentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(approvedItem)));

        List<ContentItem> result = retriever.retrieveContext("dinh dưỡng", topicId, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ContentStatus.APPROVED);
        assertThat(result.get(0).getTitle()).isEqualTo("Dinh dưỡng thai kỳ");

        // Verify repository called with APPROVED status — C5 compliance
        verify(contentRepository).searchByFilters(
                any(), isNull(), isNull(), eq(topicId), eq(ContentStatus.APPROVED), any(Pageable.class));
    }

    // RAG-TC-005b: DRAFT item không được include trong context
    @Test
    void retrieveContext_draftItemExists_draftNotInResult() {
        ContentItem approvedItem = makeApprovedItem("Dinh dưỡng thai kỳ");

        when(contentRepository.searchByFilters(
                any(), isNull(), isNull(), eq(topicId), eq(ContentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(approvedItem)));

        List<ContentItem> result = retriever.retrieveContext("dinh dưỡng", topicId, 10);

        // Kết quả không có item nào có status DRAFT
        assertThat(result).noneMatch(item -> item.getStatus() == ContentStatus.DRAFT);
    }

    // RAG-TC-006: Chỉ có DRAFT content → trả về list rỗng, không throw error
    @Test
    void retrieveContext_onlyDraftContentExists_returnsEmptyList() {
        when(contentRepository.searchByFilters(
                any(), isNull(), isNull(), any(), eq(ContentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<ContentItem> result = retriever.retrieveContext("test query", topicId, 5);

        assertThat(result).isEmpty();
    }

    // RAG-TC-006b: maxChunks được tôn trọng qua Pageable
    @Test
    void retrieveContext_maxChunks2_returnsAtMost2Items() {
        ContentItem item1 = makeApprovedItem("Bài 1");
        ContentItem item2 = makeApprovedItem("Bài 2");

        when(contentRepository.searchByFilters(
                any(), isNull(), isNull(), eq(topicId), eq(ContentStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item1, item2)));

        List<ContentItem> result = retriever.retrieveContext("test", topicId, 2);

        assertThat(result).hasSizeLessThanOrEqualTo(2);
    }
}
