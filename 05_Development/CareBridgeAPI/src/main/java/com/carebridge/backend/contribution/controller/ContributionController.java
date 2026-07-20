package com.carebridge.backend.contribution.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.contribution.dto.request.CreateContributionRequest;
import com.carebridge.backend.contribution.dto.request.UpdateContributionRequest;
import com.carebridge.backend.contribution.dto.response.ContributionResponse;
import com.carebridge.backend.contribution.entity.ContributionStatus;
import com.carebridge.backend.contribution.service.IMedicalContributionService;
import com.carebridge.backend.common.response.PaginatedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final IMedicalContributionService contributionService;

    /**
     * Create a new draft contribution.
     * Only experts with VerificationStatus.APPROVED and TrustStatus.ACTIVE can create.
     */
    @PostMapping
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ContributionResponse>> createDraft(
            @Valid @RequestBody CreateContributionRequest request,
            Principal principal) {
        UUID expertUserId = SecurityUtils.requireCurrentUserId(principal);
        ContributionResponse response = contributionService.createDraft(request, expertUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Draft contribution created successfully"));
    }

    /**
     * Get a contribution by ID.
     */
    @GetMapping("/{contributionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContributionResponse>> getById(
            @PathVariable UUID contributionId,
            Principal principal) {
        ContributionResponse response = contributionService.getById(contributionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * List my contributions with pagination.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<PaginatedResponse<ContributionResponse>>> listMyContributions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            Principal principal) {
        UUID expertUserId = SecurityUtils.requireCurrentUserId(principal);
        PaginatedResponse<ContributionResponse> response = contributionService.listMyContributions(expertUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update a draft contribution.
     * Only DRAFT contributions can be updated by their owner.
     */
    @PutMapping("/{contributionId}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ContributionResponse>> updateDraft(
            @PathVariable UUID contributionId,
            @Valid @RequestBody UpdateContributionRequest request,
            Principal principal) {
        UUID expertUserId = SecurityUtils.requireCurrentUserId(principal);
        ContributionResponse response = contributionService.updateDraft(contributionId, request, expertUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Draft contribution updated successfully"));
    }

    /**
     * Submit a draft contribution for review.
     * Transitions DRAFT -> SUBMITTED.
     */
    @PostMapping("/{contributionId}/submit")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ContributionResponse>> submitForReview(
            @PathVariable UUID contributionId,
            Principal principal) {
        UUID expertUserId = SecurityUtils.requireCurrentUserId(principal);
        ContributionResponse response = contributionService.submitForReview(contributionId, expertUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Contribution submitted for review"));
    }

    /**
     * Approve a submitted contribution (admin action).
     * Transitions SUBMITTED -> APPROVED.
     */
    @PostMapping("/{contributionId}/approve")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ContributionResponse>> approve(
            @PathVariable UUID contributionId,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        ContributionResponse response = contributionService.approve(contributionId, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Contribution approved"));
    }

    /**
     * Reject a submitted contribution (admin action).
     * Transitions SUBMITTED -> REJECTED.
     */
    @PostMapping("/{contributionId}/reject")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ContributionResponse>> reject(
            @PathVariable UUID contributionId,
            @RequestParam String reason,
            Principal principal) {
        UUID adminUserId = SecurityUtils.requireCurrentUserId(principal);
        ContributionResponse response = contributionService.reject(contributionId, adminUserId, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Contribution rejected"));
    }

    /**
     * Delete a draft contribution.
     * Only DRAFT contributions can be deleted by their owner.
     */
    @DeleteMapping("/{contributionId}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable UUID contributionId,
            Principal principal) {
        UUID expertUserId = SecurityUtils.requireCurrentUserId(principal);
        contributionService.deleteDraft(contributionId, expertUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin review queue: list contributions by status with pagination.
     */
    @GetMapping("/review-queue")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<PaginatedResponse<ContributionResponse>>> listForReview(
            @RequestParam ContributionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        PaginatedResponse<ContributionResponse> response = contributionService.listByStatus(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check if expert is eligible to create contributions.
     */
    @GetMapping("/eligibility")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<Boolean>> checkEligibility(Principal principal) {
        UUID expertUserId = SecurityUtils.requireCurrentUserId(principal);
        boolean eligible = contributionService.isEligible(expertUserId);
        return ResponseEntity.ok(ApiResponse.success(eligible));
    }
}