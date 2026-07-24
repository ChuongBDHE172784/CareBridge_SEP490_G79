package com.carebridge.backend.triage.service;

import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.TriageStage;
import java.time.Instant;
import java.util.UUID;

public record LifecycleBinding(
        UUID journeyId,
        OriginDashboard originDashboard,
        UUID originReferenceId,
        TriageStage stage,
        UUID continuationToken,
        Instant continuationExpiresAt) {}
