package com.carebridge.backend.reminder.notification.repository;

import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJob;
import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentNotificationJobRepository
        extends JpaRepository<AppointmentNotificationJob, UUID> {

    boolean existsByReminderIdAndOccurrenceIdAndConfigRevisionAndOffsetMinutes(
            UUID reminderId, UUID occurrenceId, long configRevision, int offsetMinutes);

    @Query("""
            select j.id from AppointmentNotificationJob j
             where j.status = :status
               and j.dueAt <= :now
               and j.nextAttemptAt <= :now
             order by j.dueAt, j.id
            """)
    List<UUID> findClaimableIds(
            @Param("status") AppointmentNotificationJobStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AppointmentNotificationJob j
               set j.status = :processing,
                   j.attemptCount = j.attemptCount + 1,
                   j.lockedBy = :workerId,
                   j.lockedAt = :now,
                   j.updatedAt = :now
             where j.id = :jobId
               and j.status = :pending
               and j.dueAt <= :now
               and j.nextAttemptAt <= :now
            """)
    int claim(
            @Param("jobId") UUID jobId,
            @Param("workerId") String workerId,
            @Param("now") Instant now,
            @Param("pending") AppointmentNotificationJobStatus pending,
            @Param("processing") AppointmentNotificationJobStatus processing);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AppointmentNotificationJob j
               set j.status = :pending,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :now
             where j.status = :processing
               and j.lockedAt < :cutoff
            """)
    int requeueStale(
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now,
            @Param("pending") AppointmentNotificationJobStatus pending,
            @Param("processing") AppointmentNotificationJobStatus processing);

    @Modifying
    @Query("""
            update AppointmentNotificationJob j
               set j.status = :cancelled,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :now
             where j.reminderId = :reminderId
               and j.status in :activeStatuses
            """)
    int cancelActiveByReminderId(
            @Param("reminderId") UUID reminderId,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);

    @Modifying
    @Query("""
            update AppointmentNotificationJob j
               set j.status = :cancelled,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :now
             where j.reminderId = :reminderId
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
            update AppointmentNotificationJob j
               set j.status = :cancelled,
                   j.lockedBy = null,
                   j.lockedAt = null,
                   j.updatedAt = :now
             where j.reminderId = :reminderId
               and j.configRevision <> :revision
               and j.status in :activeStatuses
            """)
    int cancelObsoleteRevisions(
            @Param("reminderId") UUID reminderId,
            @Param("revision") long revision,
            @Param("activeStatuses") Collection<AppointmentNotificationJobStatus> activeStatuses,
            @Param("cancelled") AppointmentNotificationJobStatus cancelled,
            @Param("now") Instant now);
}
