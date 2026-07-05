package com.carebridge.backend.carejourney.service;

import com.carebridge.backend.carejourney.dto.GrowthChartResponse;

import java.util.UUID;

public interface IGrowthService {

    /**
     * Returns growth chart data for a baby.
     * Measurements are sorted by measured_date ASC.
     * ageInDays is calculated from baby.birthDate in the service layer.
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-070/404) when baby not found
     * @throws com.carebridge.backend.common.exception.BusinessException (BABY-071/403) when baby not owned by user
     */
    GrowthChartResponse getGrowthChart(UUID userId, UUID babyId);
}
