package com.carebridge.backend.reminder.schedule.repository;

import com.carebridge.backend.reminder.schedule.entity.ReminderSchedule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, UUID> {
    Optional<ReminderSchedule> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    List<ReminderSchedule> findByOwnerUserIdOrderByStartDateAscCreatedAtDesc(UUID ownerUserId);

    List<ReminderSchedule> findByActiveTrueAndRecurrence(
            com.carebridge.backend.reminder.schedule.entity.ReminderScheduleRecurrence recurrence);
}
