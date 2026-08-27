package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.BabyLogSummaryResponse;

import java.security.Principal;
import java.util.UUID;

public interface IBabyLogSummaryService {

    /**
     * Returns aggregated summary of baby daily logs for the specified period.
     * Gemini AI insight is optional and fail-open (null on error).
     *
     * @param babyId  the baby profile ID
     * @param period  "24h" or "7d"
     * @param principal the authenticated user
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException (BABY-050) when baby not found
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException (BABY-051) when baby not owned
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-052) when period is invalid
     */
    BabyLogSummaryResponse getSummary(UUID babyId, String period, Principal principal);
}
