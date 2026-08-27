package com.carebridge.backend.content.dto.response;

import java.util.UUID;

// topicName is public (community_topics.name), not PII
public record TrendingTopic(UUID topicId, String topicName, long questionCount) {
}
