package com.carebridge.backend.emergency.entity;

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
@Table(name = "emergency_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "emergency_event_id")
    private UUID emergencyEventId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "action_type", length = 30)
    private String actionType;

    @Column(name = "selected_facility_id")
    private UUID selectedFacilityId;

    @Column(name = "selected_expert_id")
    private UUID selectedExpertId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
