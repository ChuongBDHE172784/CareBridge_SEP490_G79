package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;

import java.util.UUID;

public interface IJourneyService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (JOURNEY-002/409) if duplicate active journey */
    CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId);
}
