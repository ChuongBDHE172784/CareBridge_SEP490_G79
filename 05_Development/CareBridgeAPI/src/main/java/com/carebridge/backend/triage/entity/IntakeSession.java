package com.carebridge.backend.triage.entity;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import com.carebridge.backend.triage.OriginDashboard;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "triage_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntakeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "triage_session_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "baby_profile_id")
    private UUID babyProfileId;

    @Column(name = "mother_profile_id")
    private UUID motherProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = true, length = 20)
    @Builder.Default
    private TriageStage stage = TriageStage.INFANT;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_dashboard", length = 30)
    private OriginDashboard originDashboard;

    @Column(name = "origin_reference_id")
    private UUID originReferenceId;

    @Column(name = "continuation_token")
    private UUID continuationToken;

    @Column(name = "continuation_expires_at")
    private Instant continuationExpiresAt;

    @Column(name = "continuation_acknowledged_at")
    private Instant continuationAcknowledgedAt;

    @Column(name = "symptoms", nullable = false, columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @Column(name = "emergency", nullable = false)
    private boolean emergency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntakeStatus status;

    @Column(name = "disclaimer_text", columnDefinition = "TEXT")
    private String disclaimer;

    // CB-TRIAGE-CONSENT-IMP-001 (ADR-TDC-003): maps the pre-existing baseline column
    // triage_sessions.disclaimer_version (no schema change); stamped at session creation
    // once the elective-entry consent gate passes.
    @Column(name = "disclaimer_version", length = 80)
    private String disclaimerVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_jsonb", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String resultJson = "{}";

    @Column(name = "schema_version", nullable = false, length = 30)
    @Builder.Default
    private String schemaVersion = "1";

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
