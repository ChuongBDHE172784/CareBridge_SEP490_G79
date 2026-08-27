package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.response.LikeToggleResponse;
import com.carebridge.backend.community.service.CommunityAnswerLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community/answers")
@RequiredArgsConstructor
public class CommunityAnswerLikeController {

    private final CommunityAnswerLikeService likeService;

    @PostMapping("/{answerId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeToggleResponse>> toggleLike(
            @PathVariable UUID answerId,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        LikeToggleResponse response = likeService.toggleLike(userId, answerId);
        String message = response.isLiked() ? "Liked" : "Like removed";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
