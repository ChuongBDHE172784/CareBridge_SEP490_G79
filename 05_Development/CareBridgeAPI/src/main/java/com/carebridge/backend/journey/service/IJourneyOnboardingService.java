package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.dto.JourneyOnboardingStatusResponse;
import com.carebridge.backend.journey.dto.SubmitJourneyOnboardingRequest;
import java.util.UUID;

public interface IJourneyOnboardingService {
    JourneyOnboardingStatusResponse submit(UUID userId, SubmitJourneyOnboardingRequest request);
    JourneyOnboardingStatusResponse getStatus(UUID userId);
    void ensureEligible(UUID userId);
}
