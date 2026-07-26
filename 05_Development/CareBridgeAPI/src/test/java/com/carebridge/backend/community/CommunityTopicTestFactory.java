package com.carebridge.backend.community;

import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.entity.UrgencyLevel;
import java.util.UUID;
import java.util.function.Consumer;

// CASE 2.0 Props Isolation Pattern — CommunityTopicManagement_Test-Spec.md §4.
// Every test builds fresh instances via these factories; no shared mutable state.
final class CommunityTopicTestFactory {

    private CommunityTopicTestFactory() {}

    static CommunityTopic makeCategory() {
        return makeCategory(t -> {});
    }

    static CommunityTopic makeCategory(Consumer<CommunityTopic> overrides) {
        CommunityTopic topic = CommunityTopic.builder()
                .id(UUID.randomUUID())
                .name("Mang thai")
                .description("desc")
                .icon("pregnant_woman")
                .type(TopicType.CATEGORY)
                .slug("mang-thai")
                .parentId(null)
                .isHidden(false)
                .sortOrder(1)
                .createdBy(UUID.randomUUID())
                .build();
        overrides.accept(topic);
        return topic;
    }

    static CommunityTopic makeTopic(UUID categoryId) {
        return makeCategory(t -> {
            t.setId(UUID.randomUUID());
            t.setName("Dinh dưỡng thai kỳ");
            t.setIcon("restaurant");
            t.setType(TopicType.TOPIC);
            t.setSlug("dinh-duong-thai-ky");
            t.setParentId(categoryId);
        });
    }

    static CommunityTopic makeTopic() {
        return makeTopic(UUID.randomUUID());
    }

    static CommunityTopic makeTopic(Consumer<CommunityTopic> overrides) {
        CommunityTopic topic = makeTopic();
        overrides.accept(topic);
        return topic;
    }

    static CreateCommunityTopicRequest.CreateCommunityTopicRequestBuilder makeCreateTopicRequestBuilder() {
        return CreateCommunityTopicRequest.builder()
                .name("Sức khỏe tinh thần")
                .description("desc")
                .type(TopicType.CATEGORY)
                .sortOrder(0);
    }

    static CreateCommunityTopicRequest makeCreateTopicRequest(
            Consumer<CreateCommunityTopicRequest.CreateCommunityTopicRequestBuilder> overrides) {
        var builder = makeCreateTopicRequestBuilder();
        overrides.accept(builder);
        return builder.build();
    }

    static CommunityQuestion makeApprovedQuestion(UUID topicId) {
        return makeQuestion(topicId, QuestionStatus.APPROVED);
    }

    static CommunityQuestion makeQuestion(UUID topicId, QuestionStatus status) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(topicId)
                .authorId(UUID.randomUUID())
                .title("Q")
                .body("B")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(status)
                .build();
    }
}
