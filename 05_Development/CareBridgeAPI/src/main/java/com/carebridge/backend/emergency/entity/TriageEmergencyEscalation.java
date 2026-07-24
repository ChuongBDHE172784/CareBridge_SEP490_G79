package com.carebridge.backend.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "triage_emergency_escalations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageEmergencyEscalation {

    @Id
    @Column(name = "intake_session_id", nullable = false)
    private UUID intakeSessionId;

    @Column(name = "emergency_session_id", nullable = false)
    private UUID emergencySessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;
}
