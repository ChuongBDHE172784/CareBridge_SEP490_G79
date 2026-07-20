package com.carebridge.backend.community.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.community.dto.request.EditAnswerRequest;
import com.carebridge.backend.community.dto.request.PostCommunityAnswerRequest;
import com.carebridge.backend.community.dto.response.CommunityAnswerResponse;
import com.carebridge.backend.community.service.CommunityAnswerService;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
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
    private final ExpertProfileRepository expertProfileRepository;

    // ADR-COM-004: any authenticated user may post an answer
    // But EXPERT role must be verified (APPROVED) and trust ACTIVE
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityAnswerResponse>> postAnswer(
            @PathVariable UUID questionId,
            @Valid @RequestBody PostCommunityAnswerRequest request,
            Principal principal) {
        UUID authorId = SecurityUtils.requireCurrentUserId(principal);

        // If user has EXPERT role, verify they are final approved and trust active
        if (SecurityUtils.hasRole("EXPERT")) {
            boolean isVerifiedActiveExpert = expertProfileRepository.findByUserId(authorId)
                    .filter(p -> p.getVerificationStatus() == VerificationStatus.APPROVED)
                    .filter(p -> p.getTrustStatus() == com.carebridge.backend.expert.truststatus.TrustStatus.ACTIVE)
                    .isPresent();

            if (!isVerifiedActiveExpert) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Expert must be verified (APPROVED) and trust ACTIVE to post answers"));
            }
        }

        CommunityAnswerResponse response = answerService.postAnswer(authorId, questionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Answer posted"));
    }

    // UC-200: author edits own answer; status resets to PENDING for re-moderation
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommunityAnswerResponse>> editAnswer(
            @PathVariable UUID questionId,
            @PathVariable UUID id,
            @Valid @RequestBody EditAnswerRequest request,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        CommunityAnswerResponse response = answerService.editAnswer(id, callerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Answer updated"));
    }

    // UC-201: author deletes own answer (soft-delete); MODERATOR may delete any answer
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable UUID questionId,
            @PathVariable UUID id,
            Principal principal) {
        UUID callerId = SecurityUtils.requireCurrentUserId(principal);
        boolean isModeratorCaller = SecurityUtils.hasRole("MODERATOR");
        answerService.deleteAnswer(id, callerId, isModeratorCaller);
        return ResponseEntity.noContent().build();
    }
}
