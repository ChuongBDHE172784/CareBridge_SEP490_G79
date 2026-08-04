package com.carebridge.backend.reminder.schedule.entity;

import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reminder_schedule_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderScheduleJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "schedule_id", nullable = false, updatable = false)
    private UUID scheduleId;

    @Column(name = "schedule_revision", nullable = false, updatable = false)
    private long scheduleRevision;

    @Column(name = "occurrence_date", nullable = false, updatable = false)
    private LocalDate occurrenceDate;

    @Column(name = "local_time", nullable = false, updatable = false)
    private LocalTime localTime;

    @Column(name = "time_zone", nullable = false, length = 80, updatable = false)
    private String timeZone;

    @Column(name = "due_at", nullable = false, updatable = false)
    private Instant dueAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentNotificationJobStatus status = AppointmentNotificationJobStatus.PENDING;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "notification_record_id")
    private UUID notificationRecordId;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
