package com.carebridge.backend.community.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommunityQuestionSummaryResponse(
        UUID id,
        String title,
        String topicName,
        String stage,
        String urgency,
        int answerCount,
        boolean hasExpertAnswer,
        Instant createdAt
) {}
