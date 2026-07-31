package com.carebridge.backend.reminder.notification.repository;

import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentNotificationRuleRepository
        extends JpaRepository<AppointmentNotificationRule, UUID> {

    List<AppointmentNotificationRule> findByReminderIdOrderByOffsetMinutesAsc(UUID reminderId);

    void deleteByReminderId(UUID reminderId);
}
