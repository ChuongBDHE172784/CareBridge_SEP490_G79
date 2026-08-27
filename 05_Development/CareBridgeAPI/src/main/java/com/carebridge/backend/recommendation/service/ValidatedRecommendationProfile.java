package com.carebridge.backend.recommendation.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable server-normalized profile result; raw values never leave the owner service boundary. */
public record ValidatedRecommendationProfile(
        UUID submissionId,
        Map<String, Object> profile,
        Map<String, Object> derived,
        Set<String> signalSlugs,
        String canonicalProfileJson) {
}
