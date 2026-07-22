package com.carebridge.backend.expert.service;

import java.util.List;
import java.util.UUID;

public interface IContributionPointService {

    /** UC-69: Award contribution points to a user (called by other services) */
    void awardPoints(UUID userId, int points, String reason, String sourceType, UUID sourceId);

    /**
     * Award contribution points if a record with the same sourceId doesn't already exist.
     * Used for idempotent point awards on transition events (e.g., answer APPROVED).
     */
    void awardPointsIfNotExists(UUID userId, int points, String reason, String sourceType, UUID sourceId);

    /** UC-69: Total points for the authenticated user */
    int getTotalPoints(UUID userId);

    /** UC-69: Recent records for the authenticated user */
    List<com.carebridge.backend.expert.dto.response.ContributionPointResponse> getRecentPoints(UUID userId, int limit);
}
