package com.carebridge.backend.journey.service;

import java.util.UUID;

public interface ILifecycleSafetyOutcomeProjector {
    ProjectionResult ensureProjected(UUID intakeSessionId, UUID ownerUserId);
}
