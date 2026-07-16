package com.carebridge.backend.triage.entity;

import com.carebridge.backend.triage.IntakeStatus;
import com.carebridge.backend.triage.RiskLevel;
import com.carebridge.backend.triage.TriageStage;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "intake_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntakeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "baby_profile_id")
    private UUID babyProfileId;

    @Column(name = "mother_profile_id")
    private UUID motherProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    @Builder.Default
    private TriageStage stage = TriageStage.INFANT;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "symptoms", nullable = false, columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntakeStatus status;

    @Column(name = "disclaimer", columnDefinition = "TEXT")
    private String disclaimer;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
