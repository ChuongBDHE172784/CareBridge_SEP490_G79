package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.AddMilestoneRequest;
import com.carebridge.backend.carejourney.dto.MilestoneResponse;
import com.carebridge.backend.carejourney.dto.UpdateDevelopmentMilestoneRequest;

import java.util.UUID;

public interface IMilestoneService {

    /**
     * Records a developmental milestone for a baby.
     *
     * @param userId  Mother's userId from JWT
     * @param babyId  baby's UUID
     * @param request milestone details
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-060) when baby not found
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException (BABY-061) when baby not owned
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-062) when baby is archived
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-063) when milestone type is invalid
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-064) when achieved date is in future
     */
    MilestoneResponse addMilestone(UUID userId, UUID babyId, AddMilestoneRequest request);

    MilestoneResponse updateMilestone(UUID babyId, UUID milestoneId,
                                      UpdateDevelopmentMilestoneRequest request, UUID callerId);

    void deleteMilestone(UUID babyId, UUID milestoneId, UUID callerId);
}
