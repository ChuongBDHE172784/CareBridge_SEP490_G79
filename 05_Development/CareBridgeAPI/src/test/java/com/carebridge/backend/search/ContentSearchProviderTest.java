package com.carebridge.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.entity.SearchType;
import com.carebridge.backend.search.provider.ContentSearchProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ContentSearchProviderTest {

    @Mock
    private ContentService contentService;

    @Test
    void search_supportsOnlyContentType() {
        ContentSearchProvider provider = new ContentSearchProvider(contentService);

        assertThat(provider.supports(SearchType.CONTENT)).isTrue();
        assertThat(provider.supports(SearchType.QUESTION)).isFalse();
    }

    @Test
    void search_delegatesToContentService_mapsToSearchItems() {
        ContentSearchProvider provider = new ContentSearchProvider(contentService);
        Pageable pageable = PageRequest.of(0, 20);
        ContentSearchResponse content = ContentSearchResponse.builder()
                .id(UUID.randomUUID())
                .type(ContentType.ARTICLE)
                .title("Dinh dưỡng khi mang thai")
                .stage(ContentStage.PREGNANCY)
                .topicName("Dinh dưỡng")
                .publishedAt(Instant.now())
                .build();
        Page<ContentSearchResponse> page = new PageImpl<>(List.of(content), pageable, 1);
        when(contentService.searchContent(any(), any())).thenReturn(page);

        Page<SearchItemResponse> result = provider.search("dinh dưỡng", UUID.randomUUID(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(SearchType.CONTENT);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Dinh dưỡng khi mang thai");
    }
}
