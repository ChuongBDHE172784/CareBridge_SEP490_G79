package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.CreateCommunityTopicRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityTopicRequest;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.dto.response.TopicFollowResponse;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.service.CommunityTopicService;
import com.carebridge.backend.community.service.TopicFollowService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community/topics")
@RequiredArgsConstructor
public class CommunityTopicController {

    private final CommunityTopicService topicService;
    private final TopicFollowService followService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunityTopicResponse>>> getTopics(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @RequestParam(required = false) TopicType type,
            Authentication authentication) {
        boolean canManageTopics = SecurityUtils.hasRole("MODERATOR") || SecurityUtils.hasRole("CONTENT_ADMIN");
        boolean effectiveInclude = includeHidden && canManageTopics;
        UUID currentUserId = SecurityUtils.requireCurrentUserId(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(topicService.searchTopics(keyword, effectiveInclude, type, currentUserId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityTopicResponse>> createTopic(
            @Valid @RequestBody CreateCommunityTopicRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        CommunityTopicResponse response = topicService.createTopic(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Topic created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'CONTENT_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityTopicResponse>> updateTopic(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommunityTopicRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(topicService.updateTopic(id, userId, request), "Topic updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'CONTENT_ADMIN')")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id, Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        topicService.deleteTopic(id, userId);
        return ResponseEntity.noContent().build();
    }

    // UC-171: toggle follow/unfollow on a community topic
    @PostMapping("/{id}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TopicFollowResponse>> toggleFollow(
            @PathVariable UUID id,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        TopicFollowResponse response = followService.toggleFollow(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
