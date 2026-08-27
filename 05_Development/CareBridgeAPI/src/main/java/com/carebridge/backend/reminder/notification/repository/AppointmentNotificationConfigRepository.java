package com.carebridge.backend.reminder.notification.repository;

import com.carebridge.backend.reminder.notification.entity.AppointmentNotificationConfig;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentNotificationConfigRepository
        extends JpaRepository<AppointmentNotificationConfig, UUID> {
}
