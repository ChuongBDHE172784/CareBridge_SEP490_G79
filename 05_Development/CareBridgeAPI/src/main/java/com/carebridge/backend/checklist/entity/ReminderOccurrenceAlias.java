package com.carebridge.backend.checklist.entity;

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
@Table(name = "reminder_occurrence_aliases")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderOccurrenceAlias {
    @Id
    @Column(name = "occurrence_id", nullable = false, updatable = false)
    private UUID occurrenceId;

    @Column(name = "reminder_definition_id", nullable = false, updatable = false)
    private UUID reminderDefinitionId;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "scheduled_at", nullable = false, updatable = false)
    private Instant scheduledAt;

    @Column(name = "occurrence_generation", nullable = false, updatable = false)
    private long occurrenceGeneration;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
