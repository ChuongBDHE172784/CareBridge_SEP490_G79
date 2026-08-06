package com.carebridge.backend.reminder.job.entity;

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

/**
 * One row of the consolidated notification queue (V3 §3.8).
 *
 * <p>Typed-polymorphic: {@link #jobType} decides which branch of fields is
 * populated, and a database CHECK makes the two sets mutually exclusive. The
 * branch fields are nullable in Java for exactly that reason — a reminder-schedule
 * job genuinely has no {@code reminderId}.
 *
 * <p>The status enum is shared with the retired per-type queues on purpose: the
 * state machine did not change, only the table it lives in.
 */
@Entity
@Table(name = "notification_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20, updatable = false)
    private NotificationJobType jobType;

    // ---- common state machine ------------------------------------------------

    @Column(name = "due_at", nullable = false)
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

    // ---- REMINDER_SCHEDULE branch --------------------------------------------

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(name = "schedule_revision")
    private Long scheduleRevision;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "local_time")
    private LocalTime localTime;

    @Column(name = "time_zone", length = 80)
    private String timeZone;

    // ---- APPOINTMENT branch ---------------------------------------------------

    @Column(name = "reminder_id")
    private UUID reminderId;

    @Column(name = "occurrence_id")
    private UUID occurrenceId;

    /**
     * Snapshot only. Occurrence-ID v2 already folds the generation into
     * {@link #occurrenceId}, so this is not part of the appointment identity —
     * see ReminderOccurrenceIdGenerationContractTest, which guards that claim.
     */
    @Column(name = "occurrence_generation")
    private Long occurrenceGeneration;

    @Column(name = "occurrence_scheduled_at")
    private Instant occurrenceScheduledAt;

    @Column(name = "config_revision")
    private Long configRevision;

    @Column(name = "offset_minutes")
    private Integer offsetMinutes;
}
