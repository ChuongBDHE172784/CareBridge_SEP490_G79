package com.carebridge.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "structured_intake_data")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StructuredIntakeData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "symptom_list", nullable = false, columnDefinition = "jsonb")
    private String symptomList;  // stored as JSON string e.g. '["headache","fever"]'

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "intensity", length = 20)
    private String intensity;

    @Column(name = "emergency_flag", nullable = false)
    private boolean emergencyFlag;

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
