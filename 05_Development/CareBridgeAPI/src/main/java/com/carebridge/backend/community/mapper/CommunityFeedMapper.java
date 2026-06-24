package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import org.springframework.stereotype.Component;

@Component
public class CommunityFeedMapper {

    public static final String ANONYMOUS_AUTHOR = "Mẹ ẩn danh";
    private static final String UNKNOWN_AUTHOR = "Người dùng";

    public CommunityFeedItemResponse toFeedItem(CommunityQuestion q, String topicName,
                                                 String displayName, boolean hasExpertAnswer) {
        return new CommunityFeedItemResponse(
                q.getId(),
                q.getTitle(),
                topicName,
                maskAuthorIfAnonymous(q, displayName),
                q.getStage() != null ? q.getStage().name() : null,
                q.getUrgency() != null ? q.getUrgency().name() : null,
                q.getAnswerCount(),
                q.getLikeCount(),
                hasExpertAnswer,
                q.getCreatedAt()
        );
    }

    // ADR-COM-002: isAnonymous=true → "Mẹ ẩn danh"; never return null
    public String maskAuthorIfAnonymous(CommunityQuestion q, String displayName) {
        if (q.isAnonymous()) {
            return ANONYMOUS_AUTHOR;
        }
        return (displayName != null && !displayName.isBlank()) ? displayName : UNKNOWN_AUTHOR;
    }
}
