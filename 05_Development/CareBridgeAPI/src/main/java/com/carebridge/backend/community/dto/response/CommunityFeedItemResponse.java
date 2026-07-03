package com.carebridge.backend.community.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommunityFeedItemResponse(
        UUID id,
        String title,
        String topicName,
        String authorDisplay,
        String stage,
        String urgency,
        int answerCount,
        int likeCount,
        boolean hasExpertAnswer,
        boolean bookmarked,
        boolean liked,
        Instant createdAt
) {}
