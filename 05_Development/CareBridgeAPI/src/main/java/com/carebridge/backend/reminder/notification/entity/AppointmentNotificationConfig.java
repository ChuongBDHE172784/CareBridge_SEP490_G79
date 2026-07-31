package com.carebridge.backend.reminder.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "appointment_notification_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentNotificationConfig {

    @Id
    @Column(name = "reminder_id", nullable = false, updatable = false)
    private UUID reminderId;

    @Builder.Default
    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone = "Asia/Ho_Chi_Minh";

    @Builder.Default
    @Column(name = "config_revision", nullable = false)
    private long configRevision = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
