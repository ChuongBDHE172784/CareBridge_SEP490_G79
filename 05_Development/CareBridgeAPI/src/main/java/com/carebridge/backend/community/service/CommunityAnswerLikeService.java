package com.carebridge.backend.community.service;

import com.carebridge.backend.community.dto.response.LikeToggleResponse;

import java.util.UUID;

public interface CommunityAnswerLikeService {

    /**
     * Toggles like state: adds like if not liked, removes if already liked.
     * Atomically updates denormalized like_count on community_answers.
     * @throws com.carebridge.backend.community.exception.AnswerNotFoundException when answer not found
     */
    LikeToggleResponse toggleLike(UUID userId, UUID answerId);
}
