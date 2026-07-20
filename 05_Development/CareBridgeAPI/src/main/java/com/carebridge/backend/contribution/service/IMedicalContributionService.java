package com.carebridge.backend.contribution.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.contribution.dto.request.CreateContributionRequest;
import com.carebridge.backend.contribution.dto.request.UpdateContributionRequest;
import com.carebridge.backend.contribution.dto.response.ContributionResponse;
import com.carebridge.backend.contribution.entity.ContributionStatus;

import java.util.UUID;

public interface IMedicalContributionService {

    /**
     * Create a new draft contribution.
     * Only experts with VerificationStatus.APPROVED and TrustStatus.ACTIVE can create.
     */
    ContributionResponse createDraft(CreateContributionRequest request, UUID expertUserId);

    /**
     * Get a contribution by ID.
     */
    ContributionResponse getById(UUID contributionId);

    /**
     * List contributions for the current expert user.
     */
    PaginatedResponse<ContributionResponse> listMyContributions(UUID expertUserId, int page, int size);

    /**
     * List contributions by status (for admin review queue).
     */
    PaginatedResponse<ContributionResponse> listByStatus(ContributionStatus status, int page, int size);

    /**
     * Update a draft contribution.
     * Only DRAFT contributions can be updated by their owner.
     */
    ContributionResponse updateDraft(UUID contributionId, UpdateContributionRequest request, UUID expertUserId);

    /**
     * Submit a draft contribution for review.
     * Transitions DRAFT -> SUBMITTED.
     */
    ContributionResponse submitForReview(UUID contributionId, UUID expertUserId);

    /**
     * Approve a submitted contribution (admin action).
     * Transitions SUBMITTED -> APPROVED.
     */
    ContributionResponse approve(UUID contributionId, UUID adminUserId);

    /**
     * Reject a submitted contribution (admin action).
     * Transitions SUBMITTED -> REJECTED.
     */
    ContributionResponse reject(UUID contributionId, UUID adminUserId, String reason);

    /**
     * Delete a contribution (only DRAFT status).
     */
    void deleteDraft(UUID contributionId, UUID expertUserId);

    /**
     * Check if expert is eligible to create contributions.
     */
    boolean isEligible(UUID expertUserId);
}