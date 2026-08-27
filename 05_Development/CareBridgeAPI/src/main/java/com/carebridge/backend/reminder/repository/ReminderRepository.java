package com.carebridge.backend.reminder.repository;

import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<Reminder> findByOwnerUserIdOrderByScheduledAtDesc(UUID ownerUserId);

    List<Reminder> findByOwnerUserIdAndStatusNot(UUID ownerUserId, ReminderStatus status);

    Optional<Reminder> findById(UUID id);

    @Query(value = """
            SELECT status
              FROM care_tasks
             WHERE task_id = :reminderId
               AND task_type = 'SCHEDULED_REMINDER'
             FOR UPDATE
            """, nativeQuery = true)
    Optional<String> findStatusByIdForUpdate(@Param("reminderId") UUID reminderId);

    List<Reminder> findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
            UUID ownerUserId, Instant start, Instant end, List<ReminderStatus> statuses);

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.ownerUserId = :ownerUserId
              AND r.status IN :statuses
              AND (
                    (
                      r.status = com.carebridge.backend.reminder.entity.ReminderStatus.SNOOZED
                      AND r.snoozedUntil >= :start
                      AND r.snoozedUntil < :end
                    )
                    OR (
                      r.status <> com.carebridge.backend.reminder.entity.ReminderStatus.SNOOZED
                      AND r.scheduledAt >= :start
                      AND r.scheduledAt < :end
                    )
                  )
            """)
    List<Reminder> findDueTodayByOwnerAndStatusIn(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("statuses") List<ReminderStatus> statuses);

    List<Reminder> findByOwnerUserIdAndReminderTypeAndStatusIn(
            UUID ownerUserId, ReminderType reminderType, List<ReminderStatus> statuses);

    /**
     * Read-only FAMILY projection source. Appointments remain owned by the
     * mother; the selected care-group context is the only sharing boundary.
     */
    @Query("""
            SELECT r FROM Reminder r
             WHERE r.ownerUserId = :ownerUserId
               AND r.reminderType = com.carebridge.backend.reminder.entity.ReminderType.APPOINTMENT
               AND r.status <> com.carebridge.backend.reminder.entity.ReminderStatus.CANCELLED
               AND ((:journeyId IS NOT NULL AND r.journeyId = :journeyId)
                    OR (:babyId IS NOT NULL AND r.babyId = :babyId))
             ORDER BY r.scheduledAt ASC, r.id ASC
            """)
    List<Reminder> findSharedAppointments(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId);

    @Query("""
            SELECT r FROM Reminder r
               WHERE r.id = :reminderId
               AND r.ownerUserId = :ownerUserId
               AND r.reminderType = com.carebridge.backend.reminder.entity.ReminderType.APPOINTMENT
               AND ((:journeyId IS NOT NULL AND r.journeyId = :journeyId)
                    OR (:babyId IS NOT NULL AND r.babyId = :babyId))
            """)
    Optional<Reminder> findSharedAppointment(
            @Param("reminderId") UUID reminderId,
            @Param("ownerUserId") UUID ownerUserId,
            @Param("journeyId") UUID journeyId,
            @Param("babyId") UUID babyId);

    List<Reminder> findByReminderTypeAndStatusIn(
            ReminderType reminderType, List<ReminderStatus> statuses);

    /** CB-TYFU-IMP-001 — idempotency probe for BR-TYFU-002 / ADR-TYFU-003. */
    boolean existsByReminderTypeAndSourceReferenceId(ReminderType reminderType, UUID sourceReferenceId);
}
