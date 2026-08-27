package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.response.QuestionLikeToggleResponse;
import com.carebridge.backend.community.service.CommunityQuestionLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community/questions")
@RequiredArgsConstructor
public class CommunityQuestionLikeController {

    private final CommunityQuestionLikeService likeService;

    @PostMapping("/{questionId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<QuestionLikeToggleResponse>> toggleLike(
            @PathVariable UUID questionId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        QuestionLikeToggleResponse response = likeService.toggleLike(userId, questionId);
        String message = response.isLiked() ? "Liked" : "Like removed";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
