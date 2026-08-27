package com.carebridge.backend.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.RegisterDeviceTokenRequest;
import com.carebridge.backend.notification.entity.DevicePlatform;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.impl.NotificationServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeviceTokenServiceTest {

    private static final UUID USER_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final String TOKEN = "web-token";

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private NotificationServiceImpl service;

    @Test
    void registeringTokenDeactivatesAnyPreviousOwnerBeforeActivation() {
        when(deviceTokenRepository.findByUserIdAndToken(USER_ID, TOKEN)).thenReturn(Optional.empty());

        service.registerDeviceToken(USER_ID, new RegisterDeviceTokenRequest(TOKEN, DevicePlatform.WEB));

        verify(deviceTokenRepository).deactivateByTokenForOtherUsers(
                eq(USER_ID), eq(TOKEN), any());
        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    void deregisteringTokenIsOwnerScoped() {
        service.deregisterDeviceToken(USER_ID, TOKEN);

        verify(deviceTokenRepository).deactivateByUserIdAndToken(eq(USER_ID), eq(TOKEN), any());
    }
}
