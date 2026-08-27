package com.carebridge.backend.consultation.context.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "consultation_context_shares")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationContextShare {

    @Id
    @Column(name = "context_share_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "consultation_request_id", nullable = false, updatable = false)
    private UUID consultationRequestId;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "intake_session_id", nullable = false, updatable = false)
    private UUID intakeSessionId;

    @Column(name = "expert_profile_id", nullable = false, updatable = false)
    private UUID expertProfileId;

    @Column(name = "consent_grant_id", nullable = false, updatable = false)
    private Long consentGrantId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "journey_id", updatable = false)
    private UUID journeyId;

    @Column(name = "origin_dashboard", nullable = false, updatable = false, length = 30)
    private String originDashboard;

    @Column(name = "origin_reference_id", nullable = false, updatable = false)
    private UUID originReferenceId;

    @Column(name = "triage_stage", nullable = false, updatable = false, length = 20)
    private String triageStage;

    @Column(name = "risk_level", nullable = false, updatable = false, length = 10)
    private String riskLevel;

    @Column(name = "intake_status", nullable = false, updatable = false, length = 20)
    private String intakeStatus;

    @Column(name = "risk_summary", nullable = false, updatable = false, length = 500)
    private String riskSummary;

    @Column(name = "share_policy_version", nullable = false, updatable = false, length = 60)
    private String sharePolicyVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
