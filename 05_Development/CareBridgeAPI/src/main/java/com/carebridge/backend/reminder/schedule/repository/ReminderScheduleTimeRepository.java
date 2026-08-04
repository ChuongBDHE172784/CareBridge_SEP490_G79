package com.carebridge.backend.reminder.schedule.repository;

import com.carebridge.backend.reminder.schedule.entity.ReminderScheduleTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderScheduleTimeRepository extends JpaRepository<ReminderScheduleTime, UUID> {
    List<ReminderScheduleTime> findByScheduleIdOrderBySortOrderAscLocalTimeAsc(UUID scheduleId);

    void deleteByScheduleId(UUID scheduleId);
}
