package com.carebridge.backend.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "triage_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StructuredIntakeData {

    @Id
    @Column(name = "triage_session_id")
    private UUID id;

    @Column(name = "triage_session_id", nullable = false, insertable = false, updatable = false)
    private UUID sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symptom_list", nullable = false, columnDefinition = "jsonb")
    private String symptomList;  // stored as JSON string e.g. '["headache","fever"]'

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "intensity", length = 20)
    private String intensity;

    @Column(name = "emergency_flag", nullable = false)
    private boolean emergencyFlag;

    @Column(name = "emergency", nullable = false)
    private boolean emergency;

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    @Column(name = "structured_created_by", nullable = false)
    private String createdBy;
}
