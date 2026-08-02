package com.carebridge.backend.recommendation.entity;

/**
 * Lifecycle state of the owner-scoped recommendation profile stored on the
 * canonical MotherJourney row. Inactive states deliberately have no raw JSON.
 */
public enum RecommendationProfileStatus {
    NOT_STARTED,
    ACTIVE,
    DECLINED,
    REVIEW_REQUIRED,
    RECONSENT_REQUIRED,
    REVOKED
}
