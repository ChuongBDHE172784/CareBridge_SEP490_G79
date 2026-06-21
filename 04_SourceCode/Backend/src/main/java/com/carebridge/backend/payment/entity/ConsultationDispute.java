package com.carebridge.backend.payment.entity;

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
@Table(name = "consultation_disputes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dispute_id")
    private UUID disputeId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_json", columnDefinition = "jsonb")
    private String evidenceJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "resolution_type", length = 30)
    private String resolutionType;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
