package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.CreateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.request.CommunityQuestionSearchRequest;
import com.carebridge.backend.community.dto.request.UpdateCommunityQuestionRequest;
import com.carebridge.backend.community.dto.response.CommunityQuestionDetailResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionResponse;
import com.carebridge.backend.community.dto.response.CommunityQuestionSummaryResponse;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.exception.CommunityFeedValidationException;
import com.carebridge.backend.community.service.CommunityQuestionSearchService;
import com.carebridge.backend.community.service.CommunityQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community/questions")
@RequiredArgsConstructor
public class CommunityQuestionController {

    private static final int SEARCH_MAX_SIZE = 100;

    private final CommunityQuestionService questionService;
    private final CommunityQuestionSearchService searchService;

    // Mothers and family members may create questions under their own account.
    @PostMapping
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<CommunityQuestionResponse>> createQuestion(
            @Valid @RequestBody CreateCommunityQuestionRequest request,
            Principal principal) {
        UUID authorId = SecurityUtils.requireCurrentUserId(principal);
        CommunityQuestionResponse response = questionService.createQuestion(authorId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question created"));
    }

    // UC-162: any authenticated role may search APPROVED questions (ADR-COM-007, ADR-COM-008)
    @GetMapping
    public ResponseEntity<PaginatedResponse<CommunityQuestionSummaryResponse>> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) PregnancyStage stage,
            @RequestParam(required = false) Boolean hasExpertAnswer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        if (size > SEARCH_MAX_SIZE || size < 1) {
            throw new CommunityFeedValidationException(
                    "Search page size must be between 1 and " + SEARCH_MAX_SIZE + ", got: " + size);
        }

        CommunityQuestionSearchRequest request = new CommunityQuestionSearchRequest();
        request.setKeyword(keyword);
        request.setTopicId(topicId);
        request.setStage(stage);
        request.setHasExpertAnswer(hasExpertAnswer);
        request.setPage(page);
        request.setSize(size);

        UUID currentUserId = SecurityUtils.tryGetCurrentUserId(principal);

        return ResponseEntity.ok(searchService.searchQuestions(request, currentUserId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<PaginatedResponse<CommunityQuestionResponse>> getMyQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        if (page < 0 || size < 1 || size > SEARCH_MAX_SIZE) {
            throw new CommunityFeedValidationException(
                    "My questions page must be >= 0 and size between 1 and " + SEARCH_MAX_SIZE);
        }
        UUID authorId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(questionService.getMyQuestions(authorId, page, size));
    }

    // UC-199: any authenticated user may view APPROVED question detail
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityQuestionDetailResponse>> getQuestionDetail(
            @PathVariable UUID id, Principal principal) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId(principal);
        CommunityQuestionDetailResponse response = questionService.getQuestionDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public ResponseEntity<ApiResponse<CommunityQuestionResponse>> editQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCommunityQuestionRequest request,
            Principal principal) {
        UUID authorId = SecurityUtils.requireCurrentUserId(principal);
        CommunityQuestionResponse response = questionService.editQuestion(authorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Question updated"));
    }

    // UC-170: author deletes own question (soft-delete); MODERATOR may delete any question
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable UUID id,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        boolean isModeratorCaller = SecurityUtils.hasRole("MODERATOR");
        questionService.deleteQuestion(id, callerId, isModeratorCaller);
        return ResponseEntity.noContent().build();
    }
}
