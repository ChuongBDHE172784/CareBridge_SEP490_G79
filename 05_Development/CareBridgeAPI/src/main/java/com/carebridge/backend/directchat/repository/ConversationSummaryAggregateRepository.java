package com.carebridge.backend.directchat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ADR-MEDI-002 §6.2 — batch aggregate queries kept separate from the JPA repository: DISTINCT ON /
// GROUP BY over an arbitrary conversationId batch isn't expressible in plain Spring Data derived
// queries or safely in HQL. Both methods take the whole batch of ids in one call — never invoked
// per-conversation (that's exactly the N+1 pattern ADR-MEDI-002 replaces).
public interface ConversationSummaryAggregateRepository {

    record LastMessageRow(UUID messageId, String messageBody, Instant createdAt) {}

    record ReadCursor(Instant createdAt, UUID messageId) {}

    Map<UUID, LastMessageRow> fetchLastMessages(List<UUID> conversationIds);

    Map<UUID, Integer> fetchUnreadCounts(List<UUID> conversationIds, UUID currentUserId);

    ReadCursor advanceReadCursor(UUID conversationId, UUID currentUserId, boolean mother,
            Instant createdAt, UUID messageId);
}
