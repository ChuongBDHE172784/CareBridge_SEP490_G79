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
}
