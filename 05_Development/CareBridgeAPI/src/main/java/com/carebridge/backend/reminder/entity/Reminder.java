package com.carebridge.backend.reminder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_care_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "care_item_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private ReminderType reminderType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", length = 30)
    private RecurrenceType recurrenceType;

    @Column(name = "recurrence_end_date")
    private Instant recurrenceEndDate;

    @Column(name = "fcm_job_id", length = 255)
    private String fcmJobId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    // CB-TYFU-IMP-001 §5.2 — columns already exist in canonical baseline
    // B20260724111500 (:1587-1610); mapping added here, no schema change.
    @Column(name = "source_reference_type", length = 60)
    private String sourceReferenceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
