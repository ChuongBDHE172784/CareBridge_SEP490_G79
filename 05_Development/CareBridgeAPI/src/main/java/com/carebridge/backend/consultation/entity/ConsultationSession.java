package com.carebridge.backend.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Minimal, read-only mapping (UC-113) — only the columns this reporting endpoint needs.
// Approved archive compatibility mapping for the reporting endpoint.
@Entity
@Table(name = "archived_consultation_records")
@org.hibernate.annotations.SQLRestriction("legacy_table = 'consultation_sessions'")
@Getter
@Setter
@NoArgsConstructor
public class ConsultationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "archive_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "original_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "legacy_table", nullable = false, updatable = false)
    private String legacyTable = "consultation_sessions";

    @Column(name = "legacy_id", nullable = false, updatable = false)
    private String legacyId;

    @PrePersist
    void prepareArchiveIdentity() {
        legacyTable = "consultation_sessions";
        legacyId = id.toString();
    }
}
