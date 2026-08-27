package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// Minimal mapping for UC44 share-summary flow.
// only the columns this UC needs are mapped here.
@Entity
@Table(name = "consultation_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationBooking {

    @Id
    @Column(name = "booking_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "shared_summary_id")
    private UUID sharedSummaryId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    // The consultation session lives on the booking row (V3 §3.11): the free-only
    // flow allows at most one logical session per booking, so inlining the fields
    // makes that a structural invariant instead of a convention.
    @Column(name = "communication_room_id", length = 255)
    private String communicationRoomId;

    @Column(name = "session_started_at")
    private Instant sessionStartedAt;

    @Column(name = "session_ended_at")
    private Instant sessionEndedAt;

    @Column(name = "session_status", length = 30)
    private String sessionStatus;

    @Column(name = "expert_summary", columnDefinition = "text")
    private String expertSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "technical_log_json", columnDefinition = "jsonb")
    private String technicalLogJson;

    @Column(name = "session_created_at")
    private Instant sessionCreatedAt;

    /**
     * Identity of the migrated consultation_sessions row. Internal (plan §5.3):
     * kept for reconciliation and audit traceability, never exposed through a DTO,
     * and dropped in a later wave once no external reference to it remains.
     */
    @Column(name = "legacy_session_id")
    private UUID legacySessionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
