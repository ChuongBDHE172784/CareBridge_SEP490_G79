package com.carebridge.backend.integration.gemini.retriever;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.repository.ContentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentItemContextRetriever implements RagContextRetriever {

    private final ContentRepository contentRepository;

    // C5: Only APPROVED ContentItems — status is hard-coded, never from caller
    @Override
    public List<ContentItem> retrieveContext(String query, UUID topicId, int maxChunks) {
        int limit = maxChunks > 0 ? maxChunks : 5;
        Pageable pageable = PageRequest.of(0, limit);
        String keyword = (query != null && query.length() >= 3) ? query : null;

        return contentRepository
                .searchByFilters(keyword, null, null, topicId, ContentStatus.APPROVED, pageable)
                .getContent();
    }
}
