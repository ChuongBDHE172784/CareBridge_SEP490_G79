package com.carebridge.backend.triage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_source_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceSourceReviewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id")
    private UUID id;

    @Column(name = "knowledge_source_id", nullable = false)
    private UUID evidenceSourceId;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 30)
    private String newStatus;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_role", length = 80)
    private String actorRole;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
