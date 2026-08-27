package com.carebridge.backend.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Compatibility value object backed by the canonical account settings document.
 *
 * <p>V1 schema columns: preference_id, user_id, notification_type,
 * email_enabled, in_app_enabled, push_enabled, quiet_hours_start,
 * quiet_hours_end, created_at, updated_at.
 *
 * <p>UC-10 exposes push_enabled as the primary "enabled" flag per notification type.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "preference_id", updatable = false, nullable = false)
    private UUID preferenceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Notification type: REMINDER, COMMUNITY_REPLY, CONSULTATION, EMERGENCY */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Builder.Default
    @Column(name = "push_enabled")
    private Boolean pushEnabled = Boolean.TRUE;

    @Builder.Default
    @Column(name = "email_enabled")
    private Boolean emailEnabled = Boolean.TRUE;

    @Builder.Default
    @Column(name = "in_app_enabled")
    private Boolean inAppEnabled = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
