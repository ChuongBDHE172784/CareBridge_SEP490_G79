package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionSummaryResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import org.springframework.stereotype.Component;

@Component
public class CommunityQuestionMapper {

    public CommunityQuestion toEntity(CreateCommunityQuestionRequest request, Long authorId) {
        return CommunityQuestion.builder()
                .topicId(request.getTopicId())
                .authorId(authorId)
                .title(request.getTitle())
                .body(request.getBody())
                .stage(request.getStage())
                .pregnancyWeek(request.getPregnancyWeek())
                .babyAgeMonths(request.getBabyAgeMonths())
                .urgency(request.getUrgency())
                .anonymous(Boolean.TRUE.equals(request.getIsAnonymous()))
                .status(QuestionStatus.PENDING)
                .build();
    }

    public CommunityQuestionResponse toResponse(CommunityQuestion entity) {
        // ADR-COM-002: mask authorId when isAnonymous=true
        Long exposedAuthorId = entity.isAnonymous() ? null : entity.getAuthorId();

        return CommunityQuestionResponse.builder()
                .id(entity.getId())
                .topicId(entity.getTopicId())
                .title(entity.getTitle())
                .body(entity.getBody())
                .stage(entity.getStage() != null ? entity.getStage().name() : null)
                .urgency(entity.getUrgency() != null ? entity.getUrgency().name() : null)
                .anonymous(entity.isAnonymous())
                .authorId(exposedAuthorId)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // UC-162: map to lightweight summary for search results
    public CommunityQuestionSummaryResponse toSummaryResponse(
            CommunityQuestion entity, String topicName, boolean hasExpertAnswer) {
        return new CommunityQuestionSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                topicName,
                entity.getStage() != null ? entity.getStage().name() : null,
                entity.getUrgency() != null ? entity.getUrgency().name() : null,
                entity.getAnswerCount(),
                hasExpertAnswer,
                entity.getCreatedAt()
        );
    }
}
