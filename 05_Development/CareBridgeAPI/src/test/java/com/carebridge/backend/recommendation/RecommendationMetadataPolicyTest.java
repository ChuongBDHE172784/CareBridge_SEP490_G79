package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.carebridge.backend.recommendation.service.RecommendationMetadataPolicy;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationMetadataPolicyTest {

    @Test
    void acceptsCatalogIdentityAndReturnsPriority() {
        CommunityTopic topic = catalogTopic("rec-age-18-24");

        assertThat(RecommendationMetadataPolicy.validate(
                ContentType.ARTICLE, ContentStage.PREGNANCY, 12, 20, 61, List.of(topic)))
                .isEqualTo((short) 61);
    }

    @Test
    void rejectsNonArticleRecommendationMetadata() {
        assertThatThrownBy(() -> RecommendationMetadataPolicy.validate(
                ContentType.FAQ, ContentStage.PREGNANCY, null, null, 1,
                List.of(catalogTopic("rec-age-18-24"))))
                .isInstanceOf(RecommendationException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_TAG_INVALID");
    }

    @Test
    void rejectsMalformedRangeAndReservedTopicIdentity() {
        assertThatThrownBy(() -> RecommendationMetadataPolicy.validate(
                ContentType.ARTICLE, ContentStage.PREGNANCY, 20, 12, 0, List.of()))
                .isInstanceOf(RecommendationException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_WEEK_RANGE_INVALID");

        CommunityTopic malformed = catalogTopic("rec-age-18-24");
        malformed.setId(UUID.randomUUID());
        assertThatThrownBy(() -> RecommendationMetadataPolicy.validate(
                ContentType.ARTICLE, ContentStage.PREGNANCY, null, null, 0, List.of(malformed)))
                .isInstanceOf(RecommendationException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_TAG_INVALID");
    }

    private CommunityTopic catalogTopic(String slug) {
        return CommunityTopic.builder()
                .id(RecommendationConstants.catalogIdFor(slug))
                .name(slug)
                .slug(slug)
                .type(TopicType.TAG)
                .description(RecommendationConstants.CATALOG_VERSION + "|AGE")
                .build();
    }
}
