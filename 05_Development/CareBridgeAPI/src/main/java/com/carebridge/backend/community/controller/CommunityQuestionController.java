package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.service.CommunityQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/community/questions")
@RequiredArgsConstructor
public class CommunityQuestionController {

    private final CommunityQuestionService questionService;

    // ADR-COM-001: Only ROLE_MOTHER can create community questions
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CommunityQuestionResponse>> createQuestion(
            @Valid @RequestBody CreateCommunityQuestionRequest request,
            Principal principal) {
        Long authorId = SecurityUtils.requireCurrentUserId(principal);
        CommunityQuestionResponse response = questionService.createQuestion(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question created"));
    }
}
