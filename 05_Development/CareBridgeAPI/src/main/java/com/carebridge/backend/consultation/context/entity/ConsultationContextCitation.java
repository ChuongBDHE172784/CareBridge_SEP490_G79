package com.carebridge.backend.consultation.context.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "consultation_context_citations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationContextCitation {

    @Id
    @Column(name = "citation_snapshot_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "context_share_id", nullable = false, updatable = false)
    private UUID contextShareId;

    @Column(name = "evidence_source_id", nullable = false, updatable = false)
    private UUID evidenceSourceId;

    @Column(nullable = false, updatable = false, length = 255)
    private String organization;

    @Column(name = "source_url", nullable = false, updatable = false, length = 1000)
    private String sourceUrl;

    @Column(name = "source_status_at_share", nullable = false, updatable = false, length = 30)
    private String sourceStatusAtShare;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    @Column(nullable = false, updatable = false)
    private short ordinal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
