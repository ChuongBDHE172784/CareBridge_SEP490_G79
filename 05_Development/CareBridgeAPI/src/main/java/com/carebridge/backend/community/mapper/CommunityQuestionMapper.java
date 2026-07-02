package com.carebridge.backend.community.mapper;

import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionSummaryResponse;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.QuestionStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CommunityQuestionMapper {

    public CommunityQuestion toEntity(CreateCommunityQuestionRequest request, UUID authorId) {
        return CommunityQuestion.builder()
                .topicId(request.getTopicId())
                .authorId(authorId)
                .title(request.getTitle())
                .body(request.getBody())
                .stage(request.getStage())
                .pregnancyWeek(request.getPregnancyWeek() != null ? request.getPregnancyWeek().shortValue() : null)
                .babyAgeMonths(request.getBabyAgeMonths() != null ? request.getBabyAgeMonths().shortValue() : null)
                .urgency(request.getUrgency())
                .anonymous(Boolean.TRUE.equals(request.getIsAnonymous()))
                .status(QuestionStatus.PENDING)
                .build();
    }

    public CommunityQuestionResponse toResponse(CommunityQuestion entity) {
        // ADR-COM-002: mask authorId when isAnonymous=true
        UUID exposedAuthorId = entity.isAnonymous() ? null : entity.getAuthorId();

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

    // UC-199: map to full detail response including answers and topic name
    public CommunityQuestionDetailResponse toDetailResponse(
            CommunityQuestion entity, String topicName, List<CommunityAnswerResponse> answers) {
        return toDetailResponse(entity, topicName, answers, false);
    }

    // UC-58 hydration fix: "isBookmarked" reflects the CURRENT viewer's bookmark state, computed
    // by the caller — never derivable from the entity alone.
    public CommunityQuestionDetailResponse toDetailResponse(
            CommunityQuestion entity, String topicName, List<CommunityAnswerResponse> answers,
            boolean isBookmarked) {
        UUID exposedAuthorId = entity.isAnonymous() ? null : entity.getAuthorId();
        return CommunityQuestionDetailResponse.builder()
                .id(entity.getId())
                .topicId(entity.getTopicId())
                .topicName(topicName)
                .title(entity.getTitle())
                .body(entity.getBody())
                .stage(entity.getStage() != null ? entity.getStage().name() : null)
                .pregnancyWeek(entity.getPregnancyWeek())
                .babyAgeMonths(entity.getBabyAgeMonths())
                .urgency(entity.getUrgency() != null ? entity.getUrgency().name() : null)
                .anonymous(entity.isAnonymous())
                .authorId(exposedAuthorId)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .answerCount(entity.getAnswerCount())
                .likeCount(entity.getLikeCount())
                .isBookmarked(isBookmarked)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .answers(answers)
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
