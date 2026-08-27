package com.carebridge.backend.journey.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JourneyOnboardingStatusResponse {
    private final boolean baselineComplete;
    private final boolean consentValid;
    private final long baselineRevision;
    private final UUID baselineId;
    private final Long consentEvidenceId;
}
