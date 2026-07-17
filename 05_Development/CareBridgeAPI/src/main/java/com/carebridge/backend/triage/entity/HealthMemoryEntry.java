package com.carebridge.backend.triage.entity;

import com.carebridge.backend.triage.TriageStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_memory_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthMemoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "source_session_id")
    private UUID sourceSessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
