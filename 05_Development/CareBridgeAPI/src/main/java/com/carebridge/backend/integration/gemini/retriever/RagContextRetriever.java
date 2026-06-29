package com.carebridge.backend.integration.gemini.retriever;

import com.carebridge.backend.content.entity.ContentItem;
import java.util.List;
import java.util.UUID;

/**
 * Retrieves ContentItems relevant to the RAG query.
 * Only returns ContentItems with status = APPROVED.
 */
public interface RagContextRetriever {

    List<ContentItem> retrieveContext(String query, UUID topicId, int maxChunks);
}
