package com.carebridge.backend.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// COM198-TC-002: anonymous masking invariants
class CommunityFeedMapperTest {

    private CommunityFeedMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CommunityFeedMapper();
    }

    private CommunityQuestion makeQuestion(boolean isAnonymous) {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(UUID.randomUUID())
                .authorId(UUID.fromString("00000000-0000-0000-0000-000000000042"))
                .title("Test title")
                .body("Test body")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .anonymous(isAnonymous)
                .status(QuestionStatus.APPROVED)
                .build();
    }

    // COM198-TC-002a: isAnonymous=true → authorDisplay = "Mẹ ẩn danh"
    @Test
    void toFeedItem_anonymousQuestion_authorDisplayIsMasked() {
        CommunityQuestion entity = makeQuestion(true);

        CommunityFeedItemResponse response = mapper.toFeedItem(entity, "Thai kỳ", "Nguyễn Thị A", false, false);

        assertThat(response.authorDisplay()).isEqualTo(CommunityFeedMapper.ANONYMOUS_AUTHOR);
        assertThat(response.authorDisplay()).doesNotContain("Nguyễn Thị A");
        assertThat(response.authorDisplay()).doesNotContain(entity.getAuthorId().toString());
    }

    // COM198-TC-002b: isAnonymous=false → authorDisplay = real displayName
    @Test
    void toFeedItem_nonAnonymousQuestion_authorDisplayIsRealName() {
        CommunityQuestion entity = makeQuestion(false);

        CommunityFeedItemResponse response = mapper.toFeedItem(entity, "Thai kỳ", "Trần Thị B", false, false);

        assertThat(response.authorDisplay()).isEqualTo("Trần Thị B");
    }

    // COM198-TC-002c: maskAuthorIfAnonymous never returns null
    @Test
    void maskAuthorIfAnonymous_neverReturnsNull() {
        CommunityQuestion anon = makeQuestion(true);
        CommunityQuestion nonAnon = makeQuestion(false);

        assertThat(mapper.maskAuthorIfAnonymous(anon, "Real Name")).isNotNull();
        assertThat(mapper.maskAuthorIfAnonymous(nonAnon, "Real Name")).isNotNull();
    }

    // COM198-TC-002d: toFeedItem maps all fields correctly
    @Test
    void toFeedItem_mapsFieldsCorrectly() {
        CommunityQuestion entity = makeQuestion(false);
        String topicName = "Chăm sóc bé";
        String displayName = "Lê Thị C";

        CommunityFeedItemResponse response = mapper.toFeedItem(entity, topicName, displayName, true, true);

        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.title()).isEqualTo(entity.getTitle());
        assertThat(response.topicName()).isEqualTo(topicName);
        assertThat(response.authorDisplay()).isEqualTo(displayName);
        assertThat(response.stage()).isEqualTo("PREGNANCY");
        assertThat(response.urgency()).isEqualTo("NORMAL");
        assertThat(response.hasExpertAnswer()).isTrue();
        assertThat(response.bookmarked()).isTrue();
    }
}
