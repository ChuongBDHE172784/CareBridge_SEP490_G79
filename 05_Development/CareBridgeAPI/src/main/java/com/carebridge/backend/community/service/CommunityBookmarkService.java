package com.carebridge.backend.community.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.community.dto.response.BookmarkToggleResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;

import java.util.UUID;

public interface CommunityBookmarkService {

    /**
     * Toggles bookmark state: adds if not bookmarked, removes if already bookmarked.
     * @throws com.carebridge.backend.community.exception.QuestionNotFoundException when question not found
     */
    BookmarkToggleResponse toggleBookmark(UUID userId, UUID questionId);

    /**
     * Returns paginated list of APPROVED questions bookmarked by the user.
     */
    PaginatedResponse<CommunityFeedItemResponse> getBookmarkedQuestions(UUID userId, int page, int size);
}
