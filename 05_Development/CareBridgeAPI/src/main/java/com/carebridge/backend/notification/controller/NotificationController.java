package com.carebridge.backend.notification.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.RegisterDeviceTokenRequest;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/device-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        notificationService.registerDeviceToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Device token registered"));
    }

    @DeleteMapping("/device-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deregisterDeviceToken(
            @RequestParam String token,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        notificationService.deregisterDeviceToken(userId, token);
        return ResponseEntity.ok(ApiResponse.success(null, "Device token deregistered"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<NotificationRecordResponse>>> getMyNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        UUID userId = SecurityUtils.requireCurrentUserId(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationRecordResponse> result = notificationService.getMyNotifications(userId, type, pageable, principal);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<NotificationRecordResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        NotificationRecordResponse response = notificationService.send(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification sent"));
    }
}
