package com.carebridge.backend.journey.service;

import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.CreateJourneyResponse;
import com.carebridge.backend.journey.dto.JourneyResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionResponse;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.dto.RecordPregnancyOutcomeRequest;
import com.carebridge.backend.journey.dto.PregnancyOutcomeResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IJourneyTransitionService {

    /** Creates the owner's canonical mother lifecycle and initial history entry. */
    CreateJourneyResponse createJourney(CreateJourneyRequest request, UUID callerId);

    /** Updates an owned lifecycle and appends exactly one immutable transition. */
    JourneyResponse updateJourney(UUID ownerId, UUID journeyId, UpdateJourneyRequest request);

    /** Records append-only outcome evidence and applies an eligible atomic postpartum transition. */
    PregnancyOutcomeResponse recordPregnancyOutcome(
            UUID ownerId, UUID journeyId, RecordPregnancyOutcomeRequest request);

    /** Returns minimum-necessary paginated transition history for an owned lifecycle. */
    JourneyTransitionPageResponse getHistory(
            UUID ownerId, UUID journeyId, Pageable pageable);
}
