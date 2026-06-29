package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.service.CommunityAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community/questions/{questionId}/answers")
@RequiredArgsConstructor
public class CommunityAnswerController {

    private final CommunityAnswerService answerService;

    // ADR-COM-004: any authenticated user may post an answer
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityAnswerResponse>> postAnswer(
            @PathVariable UUID questionId,
            @Valid @RequestBody PostCommunityAnswerRequest request,
            Principal principal) {
        UUID authorId = SecurityUtils.requireCurrentUserId(principal);
        CommunityAnswerResponse response = answerService.postAnswer(authorId, questionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Answer posted"));
    }
}
