package com.carebridge.backend.contribution.service;

import com.carebridge.backend.contribution.dto.request.CreateContributionRequest;
import com.carebridge.backend.contribution.dto.response.ContributionResponse;
import com.carebridge.backend.contribution.entity.ContributionStatus;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

public interface MedicalContributionService {

    ContributionResponse createDraft(UUID expertUserId, CreateContributionRequest request);

    ContributionResponse getById(UUID contributionId, UUID callerId);

    ContributionResponse updateDraft(UUID contributionId, UUID expertUserId, CreateContributionRequest request);

    ContributionResponse submitForReview(UUID contributionId, UUID expertUserId);

    ContributionResponse approve(UUID contributionId, UUID adminUserId);

    ContributionResponse reject(UUID contributionId, UUID adminUserId, String reason);

    void deleteDraft(UUID contributionId, UUID expertUserId);

    List<ContributionResponse> getMyContributions(UUID expertUserId, ContributionStatus status);

    List<ContributionResponse> getContributionsForReview(UUID adminUserId);

    boolean isEligible(UUID expertUserId);
}