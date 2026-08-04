package com.carebridge.backend.reminder.schedule.repository;

import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleJob;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderScheduleJobRepository extends JpaRepository<ReminderScheduleJob, UUID> {
    boolean existsByScheduleIdAndScheduleRevisionAndOccurrenceDateAndLocalTime(
            UUID scheduleId, long scheduleRevision, java.time.LocalDate occurrenceDate,
            java.time.LocalTime localTime);

    @Query("""
            select j.id from ReminderScheduleJob j
             where j.status = :status and j.dueAt <= :now and j.nextAttemptAt <= :now
             order by j.dueAt, j.id
            """)
    List<UUID> findClaimableIds(@Param("status") AppointmentNotificationJobStatus status,
                                @Param("now") Instant now, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ReminderScheduleJob j
               set j.status = :processing, j.attemptCount = j.attemptCount + 1,
                   j.lockedBy = :workerId, j.lockedAt = :now, j.updatedAt = :now
             where j.id = :jobId and j.status = :pending
               and j.dueAt <= :now and j.nextAttemptAt <= :now
            """)
    int claim(@Param("jobId") UUID jobId, @Param("workerId") String workerId,
              @Param("now") Instant now,
              @Param("pending") AppointmentNotificationJobStatus pending,
              @Param("processing") AppointmentNotificationJobStatus processing);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ReminderScheduleJob j
               set j.status = :pending, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.status = :processing and j.lockedAt < :cutoff
            """)
    int requeueStale(@Param("cutoff") Instant cutoff, @Param("now") Instant now,
                     @Param("pending") AppointmentNotificationJobStatus pending,
                     @Param("processing") AppointmentNotificationJobStatus processing);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ReminderScheduleJob j
               set j.status = :status,
                   j.nextAttemptAt = :nextAttemptAt,
                   j.lastErrorCode = :errorCode,
                   j.notificationRecordId = :notificationRecordId,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :updatedAt
             where j.id = :jobId
               and j.status = :processing
               and j.lockedBy = :workerId
            """)
    int transitionAfterProcessing(
            @Param("jobId") UUID jobId,
            @Param("workerId") String workerId,
            @Param("processing") AppointmentNotificationJobStatus processing,
            @Param("status") AppointmentNotificationJobStatus status,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("errorCode") String errorCode,
            @Param("notificationRecordId") UUID notificationRecordId,
            @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("""
            update ReminderScheduleJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.scheduleId = :scheduleId and j.status in :activeStatuses
            """)
    int cancelActiveByScheduleId(@Param("scheduleId") UUID scheduleId,
                                 @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
                                 @Param("cancelled") AppointmentNotificationJobStatus cancelled,
                                 @Param("now") Instant now);

    @Modifying
    @Query("""
            update ReminderScheduleJob j
               set j.status = :cancelled, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
             where j.scheduleId = :scheduleId and j.scheduleRevision <> :revision
               and j.status in :activeStatuses
            """)
    int cancelObsoleteRevisions(@Param("scheduleId") UUID scheduleId, @Param("revision") long revision,
                                @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
                                @Param("cancelled") AppointmentNotificationJobStatus cancelled,
                                @Param("now") Instant now);
}
