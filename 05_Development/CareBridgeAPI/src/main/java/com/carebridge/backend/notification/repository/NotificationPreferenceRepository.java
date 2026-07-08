package com.carebridge.backend.notification.repository;

import com.carebridge.backend.notification.entity.NotificationPreference;
import com.carebridge.backend.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Find all preferences for a given user.
     */
    List<NotificationPreference> findByUserId(UUID userId);

    /**
     * Find preference for a specific user and notification type.
     */
    Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, NotificationType notificationType);

    /**
     * Delete all preferences for a user (used when account is deleted).
     */
    void deleteByUserId(UUID userId);

    /**
     * Check if push notifications are enabled for a user and type.
     * Used by UC-158 / UC-159 / UC-160 / UC-161 preference gate.
     *
     * @return true if a preference row exists with push_enabled = true,
     *         or if NO row exists (default = enabled).
     */
    @Query("""
            SELECT CASE
                WHEN COUNT(p) = 0 THEN true
                ELSE MAX(CASE WHEN p.pushEnabled = true THEN 1 ELSE 0 END) = 1
            END
            FROM NotificationPreference p
            WHERE p.userId = :userId AND p.notificationType = :type
            """)
    boolean isPushEnabled(@Param("userId") UUID userId, @Param("type") NotificationType type);
}
