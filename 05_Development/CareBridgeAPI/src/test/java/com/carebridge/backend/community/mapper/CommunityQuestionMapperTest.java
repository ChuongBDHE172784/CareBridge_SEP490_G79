package com.carebridge.backend.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CommunityQuestionMapperTest {

    private final CommunityQuestionMapper mapper = new CommunityQuestionMapper();

    private static final Long AUTHOR_ID = 2L;

    // COM-TC-002: isAnonymous=true → authorId null in response (ADR-COM-002, BR-PRIVACY)
    @Test
    void toResponse_anonymousQuestion_doesNotExposeAuthorId() {
        CommunityQuestion entity = CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .authorId(AUTHOR_ID)
                .anonymous(true)
                .status(QuestionStatus.PENDING)
                .build();

        CommunityQuestionResponse response = mapper.toResponse(entity);

        assertThat(response.getAuthorId()).isNull();
    }

    // COM-TC-002: isAnonymous=false → authorId visible in response
    @Test
    void toResponse_nonAnonymousQuestion_exposesAuthorId() {
        CommunityQuestion entity = CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .authorId(AUTHOR_ID)
                .anonymous(false)
                .status(QuestionStatus.PENDING)
                .build();

        CommunityQuestionResponse response = mapper.toResponse(entity);

        assertThat(response.getAuthorId()).isEqualTo(AUTHOR_ID);
    }

    // ADR-COM-002: entity always retains authorId regardless of isAnonymous
    @Test
    void toResponse_anonymousQuestion_entityAuthorIdStoredInDb() {
        CommunityQuestion entity = CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .authorId(AUTHOR_ID)
                .anonymous(true)
                .build();

        // DB-level: entity still carries full authorId
        assertThat(entity.getAuthorId()).isEqualTo(AUTHOR_ID);
    }

    // ADR-COM-003: status=PENDING preserved in response
    @Test
    void toResponse_pendingQuestion_statusIsPending() {
        CommunityQuestion entity = CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .status(QuestionStatus.PENDING)
                .build();

        CommunityQuestionResponse response = mapper.toResponse(entity);

        assertThat(response.getStatus()).isEqualTo("PENDING");
    }
}
