package com.carebridge.backend.reminder.repository;

import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    Optional<Reminder> findById(UUID id);

    List<Reminder> findByOwnerUserIdAndScheduledAtBetweenAndStatusIn(
            UUID ownerUserId, Instant start, Instant end, List<ReminderStatus> statuses);

    List<Reminder> findByOwnerUserIdAndReminderTypeAndStatusIn(
            UUID ownerUserId, ReminderType reminderType, List<ReminderStatus> statuses);
}
