package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.response.BookmarkToggleResponse;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.service.CommunityBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityBookmarkController {

    private final CommunityBookmarkService bookmarkService;

    @PostMapping("/questions/{questionId}/bookmark")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookmarkToggleResponse>> toggleBookmark(
            @PathVariable UUID questionId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        BookmarkToggleResponse response = bookmarkService.toggleBookmark(userId, questionId);
        String message = response.isBookmarked() ? "Bookmarked" : "Bookmark removed";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @GetMapping("/me/bookmarks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>> getBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(bookmarkService.getBookmarkedQuestions(userId, page, size));
    }
}
