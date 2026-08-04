package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.recommendation.RecommendationConstants;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Editorial validation shared by create/update and approval-time revalidation. */
public final class RecommendationMetadataPolicy {
    private RecommendationMetadataPolicy() {}

    public static short validate(ContentType type, ContentStage stage, Integer from, Integer to,
                                 Integer priority, List<CommunityTopic> topics) {
        int safePriority = priority == null ? 0 : priority;
        if (safePriority < 0 || safePriority > 100) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_PRIORITY_INVALID", "recommendation priority must be between 0 and 100");
        }
        boolean hasBounds = from != null || to != null;
        if (hasBounds && (from == null || to == null || from < 0 || from > 42 || to < 0 || to > 42 || from > to)) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_WEEK_RANGE_INVALID", "pregnancy week bounds must be inclusive and within 0..42");
        }
        if (stage != ContentStage.PREGNANCY && hasBounds) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_WEEK_RANGE_INVALID", "week bounds are allowed only for pregnancy articles");
        }
        if (type != ContentType.ARTICLE && (hasBounds || safePriority != 0 || topics.stream().anyMatch(RecommendationMetadataPolicy::isControlled))) {
            throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "RECOMMENDATION_TAG_INVALID", "recommendation metadata is allowed only for ARTICLE content");
        }
        Set<String> exclusiveGroups = new HashSet<>();
        for (CommunityTopic topic : topics) {
            if (isControlled(topic) && !isCatalogTopic(topic)) {
                throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                        "RECOMMENDATION_TAG_INVALID", "recommendation tag is not the active catalog identity");
            }
            if (isControlled(topic)) {
                String group = RecommendationConstants.EXCLUSIVE_TAG_GROUPS.get(topic.getSlug());
                if (group != null && !exclusiveGroups.add(group)) {
                    throw new RecommendationException(org.springframework.http.HttpStatus.BAD_REQUEST,
                            "RECOMMENDATION_TAG_CONFLICT", "multiple values from one exclusive audience group are not allowed");
                }
            }
        }
        return (short) safePriority;
    }

    public static boolean isControlled(CommunityTopic topic) {
        return topic != null && topic.getSlug() != null && topic.getSlug().startsWith("rec-");
    }

    public static boolean isCatalogTopic(CommunityTopic topic) {
        return topic != null
                && topic.getId() != null
                && topic.getType() == com.carebridge.backend.community.entity.TopicType.TAG
                && !topic.isHidden()
                && topic.getParentId() == null
                && topic.getSlug() != null
                && RecommendationConstants.ALL_TAG_SLUGS.contains(topic.getSlug())
                && RecommendationConstants.catalogIdFor(topic.getSlug()).equals(topic.getId())
                && topic.getDescription() != null
                && topic.getDescription().startsWith(RecommendationConstants.CATALOG_VERSION + "|");
    }
}
