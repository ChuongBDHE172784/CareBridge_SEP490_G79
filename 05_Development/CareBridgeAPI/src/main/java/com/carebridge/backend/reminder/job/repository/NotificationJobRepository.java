package com.carebridge.backend.reminder.job.repository;

import com.carebridge.backend.reminder.job.entity.NotificationJob;
import com.carebridge.backend.reminder.job.entity.NotificationJobType;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Single repository over the consolidated {@code notification_jobs} queue.
 *
 * <p>Every query carries the {@code jobType} discriminator. That is the invariant
 * the whole consolidation rests on: two independent workers now poll one table,
 * and a claim without the discriminator would let one of them lock a job whose
 * branch fields it cannot read.
 */
public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from NotificationJob j where j.id = :jobId")
    Optional<NotificationJob> findByIdForUpdate(@Param("jobId") UUID jobId);

    // ---- identity checks (one per branch) ------------------------------------

    boolean existsByJobTypeAndScheduleIdAndScheduleRevisionAndOccurrenceDateAndLocalTime(
            NotificationJobType jobType, UUID scheduleId, Long scheduleRevision,
            LocalDate occurrenceDate, LocalTime localTime);

    boolean existsByJobTypeAndReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(
            NotificationJobType jobType, UUID reminderId, UUID occurrenceId,
            Long configRevision, Integer offsetMinutes);

    // ---- claim / retry -------------------------------------------------------

    @Query("""
            select j.id from NotificationJob j
             where j.jobType = :jobType
               and j.status = :status
               and j.dueAt <= :now
               and j.nextAttemptAt <= :now
             order by j.dueAt, j.id
            """)
    List<UUID> findClaimableIds(@Param("jobType") NotificationJobType jobType,
                                @Param("status") AppointmentNotificationJobStatus status,
                                @Param("now") Instant now,
                                Pageable pageable);

    /**
     * Conditional claim: the jobType predicate means a worker cannot take a job of
     * the other type even if it were handed the id.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update NotificationJob j
               set j.status = :processing,
                   j.attemptCount = j.attemptCount + 1,
                   j.lockedBy = :workerId,
                   j.lockedAt = :now,
                   j.updatedAt = :now
             where j.id = :jobId
               and j.jobType = :jobType
               and j.status = :pending
               and j.dueAt <= :now
               and j.nextAttemptAt <= :now
            """)
    int claim(@Param("jobId") UUID jobId,
              @Param("jobType") NotificationJobType jobType,
              @Param("workerId") String workerId,
              @Param("now") Instant now,
              @Param("pending") AppointmentNotificationJobStatus pending,
              @Param("processing") AppointmentNotificationJobStatus processing);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update NotificationJob j
               set j.status = :pending,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :now
             where j.jobType = :jobType
               and j.status = :processing
               and j.lockedAt < :cutoff
            """)
    int requeueStale(@Param("jobType") NotificationJobType jobType,
                     @Param("cutoff") Instant cutoff,
                     @Param("now") Instant now,
                     @Param("pending") AppointmentNotificationJobStatus pending,
                     @Param("processing") AppointmentNotificationJobStatus processing);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update NotificationJob j
               set j.status = :status,
                   j.nextAttemptAt = :nextAttemptAt,
                   j.lastErrorCode = :errorCode,
                   j.notificationRecordId = :notificationRecordId,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :updatedAt
             where j.id = :jobId
               and j.jobType = :jobType
               and j.status = :processing
               and j.lockedBy = :workerId
            """)
    int transitionAfterProcessing(
            @Param("jobId") UUID jobId,
            @Param("jobType") NotificationJobType jobType,
            @Param("workerId") String workerId,
            @Param("processing") AppointmentNotificationJobStatus processing,
            @Param("status") AppointmentNotificationJobStatus status,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("errorCode") String errorCode,
            @Param("notificationRecordId") UUID notificationRecordId,
            @Param("updatedAt") Instant updatedAt);

    // ---- cancellation: REMINDER_SCHEDULE branch ------------------------------

    @Modifying
    @Query("""
            update NotificationJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.jobType = com.carebridge.backend.reminder.job.entity.NotificationJobType.REMINDER_SCHEDULE
               and j.scheduleId = :scheduleId
               and j.status in :activeStatuses
            """)
    int cancelActiveByScheduleId(
            @Param("scheduleId") UUID scheduleId,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            update NotificationJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.jobType = com.carebridge.backend.reminder.job.entity.NotificationJobType.REMINDER_SCHEDULE
               and j.scheduleId = :scheduleId
               and j.scheduleRevision <> :revision
               and j.status in :activeStatuses
            """)
    int cancelObsoleteScheduleRevisions(
            @Param("scheduleId") UUID scheduleId,
            @Param("revision") long revision,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);

    // ---- cancellation: APPOINTMENT branch ------------------------------------

    @Modifying
    @Query("""
            update NotificationJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.jobType = com.carebridge.backend.reminder.job.entity.NotificationJobType.APPOINTMENT
               and j.reminderId = :reminderId
               and j.status in :activeStatuses
            """)
    int cancelActiveByReminderId(
            @Param("reminderId") UUID reminderId,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            update NotificationJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.jobType = com.carebridge.backend.reminder.job.entity.NotificationJobType.APPOINTMENT
               and j.reminderId = :reminderId
               and j.occurrenceId = :occurrenceId
               and j.status in :activeStatuses
            """)
    int cancelActiveByOccurrenceId(
            @Param("reminderId") UUID reminderId,
            @Param("occurrenceId") UUID occurrenceId,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            update NotificationJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.jobType = com.carebridge.backend.reminder.job.entity.NotificationJobType.APPOINTMENT
               and j.reminderId = :reminderId
               and j.configRevision <> :revision
               and j.status in :activeStatuses
            """)
    int cancelObsoleteConfigRevisions(
            @Param("reminderId") UUID reminderId,
            @Param("revision") long revision,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);

    long countByJobTypeAndStatus(NotificationJobType jobType, AppointmentNotificationJobStatus status);
}
