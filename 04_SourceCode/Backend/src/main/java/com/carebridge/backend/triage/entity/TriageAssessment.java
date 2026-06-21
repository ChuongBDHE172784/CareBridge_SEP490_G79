package com.carebridge.backend.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "triage_assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "symptom_summary", columnDefinition = "TEXT")
    private String symptomSummary;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "rule_version", length = 50)
    private String ruleVersion;

    @Column(name = "disclaimer_accepted")
    private Boolean disclaimerAccepted;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
