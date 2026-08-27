package com.carebridge.backend.journey.entity;

import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleSafetyOutcome {
    private UUID id;
    private UUID ownerUserId;
    private UUID journeyId;
    private UUID intakeSessionId;
    private UUID emergencySessionId;
    private RiskLevel riskLevel;
    private TriageStage stage;
    private OriginDashboard originDashboard;
    private UUID originReferenceId;
    private OriginAction originAction;
    private Instant occurredAt;
    private Instant recordedAt;
}
