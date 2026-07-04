package com.carebridge.backend.search.provider;

import com.carebridge.backend.content.dto.request.ContentSearchRequest;
import com.carebridge.backend.content.dto.response.ContentSearchResponse;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.search.dto.response.SearchItemResponse;
import com.carebridge.backend.search.entity.SearchType;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * UC-13 search provider for the verified content library.
 *
 * <p>Delegates to {@link ContentService#searchContent} (built for UC-224) which already
 * enforces status=APPROVED visibility and uses bind params. {@link ContentSearchResponse}
 * carries no body/description field (BR-PRIVACY-safe summary only), so the snippet here is
 * the title itself — there is no separate long-form text to truncate for this domain.
 */
@Component
@RequiredArgsConstructor
public class ContentSearchProvider implements DomainSearchProvider {

    private final ContentService contentService;

    @Override
    public boolean supports(SearchType type) {
        return type == SearchType.CONTENT;
    }

    @Override
    public Page<SearchItemResponse> search(String q, UUID userId, Pageable pageable) {
        ContentSearchRequest request = new ContentSearchRequest();
        request.setKeyword(q);
        Page<ContentSearchResponse> page = contentService.searchContent(request, pageable);
        return page.map(this::toItem);
    }

    private SearchItemResponse toItem(ContentSearchResponse content) {
        return SearchItemResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .snippet(content.getTitle())
                .type(SearchType.CONTENT)
                .metadata(Map.of(
                        "contentType", String.valueOf(content.getType()),
                        "stage", String.valueOf(content.getStage())))
                .createdAt(content.getPublishedAt())
                .build();
    }
}
