package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated Mother's exercise context.
 *
 * <p>The exercise domain still exposes the optional legacy {@code journeyId} request field,
 * while canonical health observations require a non-null {@code care_subject_id}. This
 * resolver keeps that translation server-owned and validates an explicitly supplied journey
 * against the JWT owner before returning either identifier.
 */
@Component
@RequiredArgsConstructor
public class ExerciseCareContextResolver {

    private final MotherJourneyRepository motherJourneyRepository;

    public CareContext resolve(UUID ownerUserId, UUID requestedJourneyId) {
        MotherJourney journey = requestedJourneyId == null
                ? motherJourneyRepository.findCanonical(ownerUserId)
                        .orElseThrow(this::missingContext)
                : motherJourneyRepository
                        .findByIdAndOwnerUserIdAndStatus(
                                requestedJourneyId, ownerUserId, JourneyStatus.ACTIVE)
                        .orElseThrow(this::missingContext);

        if (journey.getId() == null || journey.getCareSubjectId() == null) {
            throw missingContext();
        }
        return new CareContext(journey.getId(), journey.getCareSubjectId());
    }

    private BusinessException missingContext() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "EXERCISE_JOURNEY_REQUIRED",
                "An active maternal journey is required before starting an exercise.");
    }

    public record CareContext(UUID journeyId, UUID careSubjectId) {
    }
}
