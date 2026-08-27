package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyDashboardResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IJourneyService {

    /** UC22 — Create a new mother journey. */
    CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId);

    /**
     * UC23 — Update an existing mother journey.
     *
     * @throws com.carebridge.backend.common.exception.BusinessException JOURNEY-010 (404) not found
     * @throws com.carebridge.backend.common.exception.BusinessException JOURNEY-011 (403) not owner
     * @throws com.carebridge.backend.common.exception.BusinessException JOURNEY-012 (400) not ACTIVE
     * @throws com.carebridge.backend.common.exception.BusinessException JOURNEY-013 (400) COMPLETED without deliveryDate
     * @throws com.carebridge.backend.common.exception.BusinessException JOURNEY-014 (400) ARCHIVED status attempted
     */
    JourneyResponse updateJourney(UUID ownerId, UUID journeyId, UpdateJourneyRequest request);

    /** Returns minimum-necessary paginated history for an owned journey. */
    JourneyTransitionPageResponse getHistory(
            UUID ownerId, UUID journeyId, Pageable pageable);

    /**
     * UC24 — Return dashboard for the authenticated mother.
     * Always returns HTTP 200. If no active journey, returns status=NO_JOURNEY with all fields null.
     */
    JourneyDashboardResponse getDashboard(UUID userId);
}
