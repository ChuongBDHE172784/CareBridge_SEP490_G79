package com.carebridge.backend.triage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceSource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String domain;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false, length = 255)
    private String organization;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "discovery_mode", nullable = false, length = 40)
    private String discoveryMode;

    @Column(name = "applicable_stages", nullable = false, columnDefinition = "TEXT")
    private String applicableStages;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
