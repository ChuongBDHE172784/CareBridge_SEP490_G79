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
import java.util.ArrayList;
import java.util.UUID;

@Component
public class CommunityQuestionMapper {

    private static final String ANONYMOUS_AUTHOR = "Thành viên ẩn danh";

    public CommunityQuestion toEntity(CreateCommunityQuestionRequest request, UUID authorId) {
        return CommunityQuestion.builder()
                .topicId(request.getTopicId())
                .authorId(authorId)
                .title(request.getTitle())
                .body(request.getBody())
                .imageUrls(copyImageUrls(request.getImageUrls()))
                .stage(request.getStage())
                .pregnancyWeek(request.getPregnancyWeek() != null ? request.getPregnancyWeek().shortValue() : null)
                .babyAgeMonths(request.getBabyAgeMonths() != null ? request.getBabyAgeMonths().shortValue() : null)
                .urgency(request.getUrgency())
                .anonymous(Boolean.TRUE.equals(request.getIsAnonymous()))
                .status(QuestionStatus.AI_PENDING)
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
                .imageUrls(copyImageUrls(entity.getImageUrls()))
                .stage(entity.getStage() != null ? entity.getStage().name() : null)
                .urgency(entity.getUrgency() != null ? entity.getUrgency().name() : null)
                .anonymous(entity.isAnonymous())
                .authorId(exposedAuthorId)
                .status(toResponseStatus(entity.getStatus()))
                .answerCount(entity.getAnswerCount())
                .likeCount(entity.getLikeCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // UC-199: map to full detail response including answers and topic name
    public CommunityQuestionDetailResponse toDetailResponse(
            CommunityQuestion entity, String topicName, List<CommunityAnswerResponse> answers) {
        return toDetailResponse(entity, topicName, null, answers, false, false, null);
    }

    // UC-58 hydration fix: "isBookmarked" reflects the CURRENT viewer's bookmark state, computed
    // by the caller — never derivable from the entity alone.
    public CommunityQuestionDetailResponse toDetailResponse(
            CommunityQuestion entity, String topicName, List<CommunityAnswerResponse> answers,
            boolean isBookmarked, boolean isLiked) {
        return toDetailResponse(entity, topicName, null, answers, isBookmarked, isLiked, null);
    }

    // authorId/authorDisplay masking for anonymous questions must stay viewer-aware: the AUTHOR
    // themselves still needs their own raw authorId back (mobile's "is this my question" /
    // edit-button check depends on it), everyone else gets it nulled out per ADR-COM-002.
    public CommunityQuestionDetailResponse toDetailResponse(
            CommunityQuestion entity, String topicName, String authorDisplay, List<CommunityAnswerResponse> answers,
            boolean isBookmarked, boolean isLiked, UUID currentUserId) {
        boolean viewerIsAuthor = currentUserId != null && currentUserId.equals(entity.getAuthorId());
        UUID exposedAuthorId = (entity.isAnonymous() && !viewerIsAuthor) ? null : entity.getAuthorId();
        String finalAuthorDisplay = entity.isAnonymous() ? ANONYMOUS_AUTHOR
                : ((authorDisplay != null && !authorDisplay.isBlank()) ? authorDisplay : "Người dùng");
        return CommunityQuestionDetailResponse.builder()
                .id(entity.getId())
                .topicId(entity.getTopicId())
                .topicName(topicName)
                .title(entity.getTitle())
                .body(entity.getBody())
                .imageUrls(copyImageUrls(entity.getImageUrls()))
                .stage(entity.getStage() != null ? entity.getStage().name() : null)
                .pregnancyWeek(entity.getPregnancyWeek())
                .babyAgeMonths(entity.getBabyAgeMonths())
                .urgency(entity.getUrgency() != null ? entity.getUrgency().name() : null)
                .anonymous(entity.isAnonymous())
                .authorId(exposedAuthorId)
                .authorDisplay(finalAuthorDisplay)
                .status(toResponseStatus(entity.getStatus()))
                .answerCount(entity.getAnswerCount())
                .likeCount(entity.getLikeCount())
                .isBookmarked(isBookmarked)
                .isLiked(isLiked)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .answers(answers)
                .build();
    }

    private List<String> copyImageUrls(List<String> imageUrls) {
        return imageUrls == null ? new ArrayList<>() : new ArrayList<>(imageUrls);
    }

    private String toResponseStatus(QuestionStatus status) {
        if (status == null) {
            return null;
        }
        return status == QuestionStatus.AI_PENDING ? QuestionStatus.PENDING.name() : status.name();
    }

    // UC-162: map to summary for search results
    public CommunityQuestionSummaryResponse toSummaryResponse(
            CommunityQuestion entity, String topicName, boolean hasExpertAnswer) {
        return toSummaryResponse(entity, topicName, null, hasExpertAnswer, false, false);
    }

    public CommunityQuestionSummaryResponse toSummaryResponse(
            CommunityQuestion entity, String topicName, String authorDisplay,
            boolean hasExpertAnswer, boolean isBookmarked, boolean isLiked) {
        String finalAuthorDisplay = entity.isAnonymous() ? ANONYMOUS_AUTHOR
                : ((authorDisplay != null && !authorDisplay.isBlank()) ? authorDisplay : "Người dùng");
        return new CommunityQuestionSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                topicName,
                finalAuthorDisplay,
                entity.getStage() != null ? entity.getStage().name() : null,
                entity.getUrgency() != null ? entity.getUrgency().name() : null,
                entity.getAnswerCount(),
                entity.getLikeCount(),
                hasExpertAnswer,
                isBookmarked,
                isLiked,
                entity.getCreatedAt()
        );
    }
}
