package com.carebridge.backend.triage.entity;

import com.carebridge.backend.triage.TriageStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_context_memories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthMemoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "memory_id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mother_profile_id")
    private UUID motherProfileId;

    @Column(name = "baby_profile_id")
    private UUID babyProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_stage", nullable = false, length = 20)
    private TriageStage relatedStage;

    /** Processed/minimized summary only. Raw conversation text is never stored here. */
    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    /**
     * Structured, minimized payload (application-owned schema, {@code schemaVersion "1.0"} —
     * CB-TRIAGE-THMC-IMP-001 §5.2). Maps the pre-existing {@code memory_payload_jsonb} column
     * (canonical baseline :1005); no schema change (ADR-THMC-004, Logic Issue L1).
     * Same minimization rule as {@link #summaryText}: never raw free text.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "memory_payload_jsonb", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String memoryPayloadJson = "{}";

    @Column(name = "triage_session_id")
    private UUID sourceSessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
