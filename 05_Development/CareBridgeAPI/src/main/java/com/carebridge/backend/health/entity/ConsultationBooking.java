package com.carebridge.backend.health.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

// Minimal mapping for UC44 share-summary flow.
// The approved archive keeps queryable booking columns for this compatibility flow.
// only the columns this UC needs are mapped here.
@Entity
@Table(name = "archived_consultation_records")
@org.hibernate.annotations.SQLRestriction("legacy_table = 'consultation_bookings'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationBooking {

    @Id
    @Column(name = "archive_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "shared_summary_id")
    private UUID sharedSummaryId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @CreationTimestamp
    @Column(name = "original_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "legacy_table", nullable = false, updatable = false)
    private String legacyTable = "consultation_bookings";

    @Column(name = "legacy_id", nullable = false, updatable = false)
    private String legacyId;

    @PrePersist
    void prepareArchiveIdentity() {
        legacyTable = "consultation_bookings";
        legacyId = id.toString();
    }
}
