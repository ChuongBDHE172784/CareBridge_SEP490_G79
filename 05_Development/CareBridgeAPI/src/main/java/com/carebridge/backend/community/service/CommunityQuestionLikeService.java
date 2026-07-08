package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.response.QuestionLikeToggleResponse;

import java.util.UUID;

public interface CommunityQuestionLikeService {

    /**
     * Toggles the like state for a community question.
     * Adds like and increments like_count if not yet liked.
     * Removes like and decrements like_count if already liked.
     * Both the like record and question.likeCount are updated atomically (@Transactional).
     *
     * @param userId     UUID of the authenticated user (from JWT)
     * @param questionId UUID of the target question
     * @return QuestionLikeToggleResponse with the new like state and updated likeCount
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException (COM-006)
     *         when questionId does not exist in DB
     */
    QuestionLikeToggleResponse toggleLike(UUID userId, UUID questionId);
}
