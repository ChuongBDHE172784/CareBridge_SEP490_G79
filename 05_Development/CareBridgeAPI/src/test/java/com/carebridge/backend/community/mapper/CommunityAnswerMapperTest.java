package com.carebridge.backend.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.entity.CommunityAnswer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommunityAnswerMapperTest {

    private final CommunityAnswerMapper mapper = new CommunityAnswerMapper();

    @Test
    void toEntity_answerWithCloudinaryImages_preservesUrls() {
        List<String> imageUrls = List.of(
                "https://res.cloudinary.com/carebridge/image/upload/answer-1.jpg");
        PostCommunityAnswerRequest request = new PostCommunityAnswerRequest();
        request.setBody("This answer contains enough characters");
        request.setIsPersonalExperience(false);
        request.setImageUrls(imageUrls);

        CommunityAnswer entity = mapper.toEntity(
                request, UUID.randomUUID(), UUID.randomUUID(), true);
        CommunityAnswerResponse response = mapper.toResponse(entity);

        assertThat(entity.getImageUrls()).containsExactlyElementsOf(imageUrls);
        assertThat(response.getImageUrls()).containsExactlyElementsOf(imageUrls);
        assertThat(entity.getImageUrls()).isNotSameAs(imageUrls);
    }

    @Test
    void toResponse_expertLabeled_preservesAuthorDisplayNameAndExpertProfileId() {
        UUID authorId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID expertProfileId = UUID.randomUUID();
        CommunityAnswer entity = CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .authorId(authorId)
                .questionId(questionId)
                .body("Câu trả lời chuyên môn từ bác sĩ")
                .expertLabeled(true)
                .build();

        CommunityAnswerResponse response = mapper.toResponse(
                entity, "BS. CKII Nguyễn Văn A", false, expertProfileId);

        assertThat(response.isExpertLabeled()).isTrue();
        assertThat(response.getAuthorDisplay()).isEqualTo("BS. CKII Nguyễn Văn A");
        assertThat(response.getExpertProfileId()).isEqualTo(expertProfileId);
    }

    @Test
    void toResponse_expertLabeledWithoutAuthorDisplay_fallsBackToChuyenGia() {
        UUID expertProfileId = UUID.randomUUID();
        CommunityAnswer entity = CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .questionId(UUID.randomUUID())
                .body("Câu trả lời chuyên môn")
                .expertLabeled(true)
                .build();

        CommunityAnswerResponse response = mapper.toResponse(
                entity, null, false, expertProfileId);

        assertThat(response.isExpertLabeled()).isTrue();
        assertThat(response.getAuthorDisplay()).isEqualTo("Chuyên gia");
        assertThat(response.getExpertProfileId()).isEqualTo(expertProfileId);
    }
}
