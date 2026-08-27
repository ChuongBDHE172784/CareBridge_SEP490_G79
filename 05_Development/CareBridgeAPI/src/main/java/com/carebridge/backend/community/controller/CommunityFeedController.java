package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.response.CommunityFeedItemResponse;
import com.carebridge.backend.community.exception.CommunityFeedValidationException;
import com.carebridge.backend.community.service.CommunityFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityFeedController {

    private static final int FEED_MAX_SIZE = 50;

    private final CommunityFeedService feedService;

    @GetMapping("/feed")
    public ResponseEntity<PaginatedResponse<CommunityFeedItemResponse>> getFeed(
            @RequestParam(required = false) UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        if (size > FEED_MAX_SIZE || size < 1) {
            throw new CommunityFeedValidationException(
                    "Feed page size must be between 1 and " + FEED_MAX_SIZE + ", got: " + size);
        }

        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        PaginatedResponse<CommunityFeedItemResponse> response = feedService.getFeed(topicId, currentUserId, page, size);
        return ResponseEntity.ok(response);
    }
}
