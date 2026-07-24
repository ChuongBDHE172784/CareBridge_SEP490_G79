package com.carebridge.backend.journey.entity;

import com.carebridge.backend.triage.OriginAction;
import com.carebridge.backend.triage.OriginDashboard;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lifecycle_safety_outcomes")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleSafetyOutcome {
    @Id
    @Column(name = "outcome_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "journey_id", nullable = false, updatable = false)
    private UUID journeyId;

    @Column(name = "intake_session_id", nullable = false, updatable = false)
    private UUID intakeSessionId;

    @Column(name = "emergency_session_id", updatable = false)
    private UUID emergencySessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, updatable = false)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, updatable = false)
    private TriageStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_dashboard", nullable = false, updatable = false)
    private OriginDashboard originDashboard;

    @Column(name = "origin_reference_id", nullable = false, updatable = false)
    private UUID originReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_action", nullable = false, updatable = false)
    private OriginAction originAction;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
