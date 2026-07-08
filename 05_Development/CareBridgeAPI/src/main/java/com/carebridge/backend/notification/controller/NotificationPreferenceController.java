package com.carebridge.backend.notification.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.notification.dto.NotificationPreferencesResponse;
import com.carebridge.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.carebridge.backend.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * UC-10: Update Notification Preferences.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET  /api/v1/users/me/notification-preferences — retrieve current preferences</li>
 *   <li>PUT  /api/v1/users/me/notification-preferences — update (upsert) preferences</li>
 * </ul>
 *
 * <p>Controller responsibilities: validate request, extract userId from JWT, delegate to service.
 * No business logic here (CASE 2.0 C6 / AP-AI-001).
 */
@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;

    /**
     * GET /api/v1/users/me/notification-preferences
     * Retrieve the current user's notification preferences.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> getPreferences(
            Principal principal) {

        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        NotificationPreferencesResponse response = notificationPreferenceService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification preferences retrieved successfully"));
    }

    /**
     * PUT /api/v1/users/me/notification-preferences
     * Upsert notification preferences for the current user.
     * userId is always taken from JWT — never from the request body (C1).
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponse>> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request,
            Principal principal) {

        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        NotificationPreferencesResponse response =
                notificationPreferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Notification preferences updated successfully"));
    }
}
