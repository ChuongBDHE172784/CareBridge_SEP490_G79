package com.carebridge.backend.integration.gemini.retriever;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.content.entity.ContentStatus;
import com.carebridge.backend.content.entity.ContentStage;
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
        return retrieve(query, topicId, null, maxChunks);
    }

    @Override
    public List<ContentItem> retrieveContext(
            String query, UUID topicId, ContentStage canonicalStage, int maxChunks) {
        return retrieve(query, topicId, canonicalStage, maxChunks);
    }

    private List<ContentItem> retrieve(
            String query, UUID topicId, ContentStage stage, int maxChunks) {
        int limit = maxChunks > 0 ? maxChunks : 5;
        Pageable pageable = PageRequest.of(0, limit);
        // Triage appends the canonical stage to the generation query.  Keep that stage in the
        // Gemini prompt, but search approved content by the actual symptom text so a title/body
        // does not need to contain the literal marker (for example, "giai đoạn pregnancy").
        String retrievalQuery = query;
        if (retrievalQuery != null) {
            int stageMarker = retrievalQuery.toLowerCase(java.util.Locale.ROOT)
                    .indexOf("; giai đoạn ");
            if (stageMarker >= 0) {
                retrievalQuery = retrievalQuery.substring(0, stageMarker).trim();
            }
            int additionalFacts = retrievalQuery.indexOf(';');
            if (additionalFacts >= 0) {
                // The first segment is the user's symptom text. Structured duration/vitals
                // remain in the Gemini prompt but must not make SQL LIKE require one giant
                // exact phrase that approved article titles cannot contain.
                retrievalQuery = retrievalQuery.substring(0, additionalFacts).trim();
            }
        }
        String keyword = (retrievalQuery != null && retrievalQuery.length() >= 3)
                ? retrievalQuery : null;

        return contentRepository
                .searchByFilters(keyword, null, stage, topicId, ContentStatus.APPROVED, pageable)
                .getContent();
    }
}
