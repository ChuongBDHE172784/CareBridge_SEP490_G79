package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.consent.entity.ConsentGrant;
import java.util.UUID;

public interface RecommendationConsentCleanup {
    void onRevoked(UUID ownerUserId, ConsentGrant grant);
}
