package com.carebridge.backend.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reminders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reminder_id")
    private UUID reminderId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "baby_id")
    private UUID babyId;

    @Column(name = "reminder_type", length = 40)
    private String reminderType;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "recurrence_rule", length = 255)
    private String recurrenceRule;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
