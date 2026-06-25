package com.carebridge.backend.consultation.entity;

import com.carebridge.backend.expert.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Consultation dispute entity.
 * Tracks disputes raised during or after consultations.
 *
 * Can lead to refunds.
 */
@Entity
@Table(name = "consultation_disputes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id")
    private Long disputeId;

    /**
     * Foreign key to consultation_bookings table.
     */
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /**
     * User who submitted the dispute.
     * Either the requester or the expert.
     */
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    /**
     * Admin who resolved the dispute.
     */
    @Column(name = "resolved_by")
    private Long resolvedBy;

    /**
     * Dispute reason code (business rule).
     */
    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;

    /**
     * Detailed description of the dispute.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Evidence files (JSON array of URLs).
     */
    @Column(name = "evidence_json", columnDefinition = "JSONB")
    private String evidenceJson;

    /**
     * Current dispute status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DisputeStatus status;

    /**
     * Resolution type if applicable: REFUND, CREDIT, NO_ACTION.
     */
    @Column(name = "resolution_type", length = 50)
    private String resolutionType;

    /**
     * Admin notes about the resolution.
     */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /**
     * When the dispute was submitted.
     */
    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();

    /**
     * When the dispute was resolved.
     */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
