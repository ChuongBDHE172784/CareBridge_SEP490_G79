package com.carebridge.backend.notification.service;

import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.RegisterDeviceTokenRequest;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request);

    void deregisterDeviceToken(UUID userId, String token);

    NotificationRecordResponse send(SendNotificationRequest request);

    Page<NotificationRecordResponse> getMyNotifications(UUID userId, String type, Pageable pageable, Principal principal);

    // =========================================================================
    // UC-12: Mark as Read
    // =========================================================================

    /**
     * Mark a single notification as read (UC-12).
     * Idempotent: if already read, returns without error.
     *
     * @param userId         user ID from JWT
     * @param notificationId notification to mark
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException NOTIF-020
     *         if notification not found or not owned by userId
     */
    void markAsRead(UUID userId, UUID notificationId);

    /**
     * Mark all unread notifications of a user as read (UC-12).
     *
     * @param userId user ID from JWT
     * @return number of notifications marked as read (0 if all already read)
     */
    int markAllAsRead(UUID userId);
}
