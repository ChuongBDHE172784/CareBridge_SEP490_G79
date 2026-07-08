package com.carebridge.backend.notification;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationRecordStatus;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests confirming UC-11 View Notifications via GET /api/v1/notifications/me.
 *
 * UC-11 is already implemented — these tests verify ownership enforcement and pagination.
 * TDS Reference: CB-NOTIF-IMP-011
 */
@ExtendWith(MockitoExtension.class)
class NotificationViewServiceTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private NotificationServiceImpl service;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    // =========================================================================
    // NOTIF-TC-011-001: Happy path — get all notifications for userId
    // =========================================================================

    @Test
    @DisplayName("NOTIF-TC-011-001: getMyNotifications returns page for current user only")
    void getMyNotifications_withValidUser_returnsPage() {
        // Given
        NotificationRecord record = buildRecord(USER_ID, NotificationType.REMINDER);
        Page<NotificationRecord> page = new PageImpl<>(List.of(record));
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationRecordRepository.findByUserId(eq(USER_ID), eq(pageable))).thenReturn(page);

        // When
        Page<NotificationRecordResponse> result = service.getMyNotifications(USER_ID, null, pageable, null);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo(USER_ID);

        // Ownership: query is always scoped to USER_ID — no call with other userId
        verify(notificationRecordRepository).findByUserId(eq(USER_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("NOTIF-TC-011-002: getMyNotifications with type filter uses typed repository method")
    void getMyNotifications_withTypeFilter_usesTypedQuery() {
        // Given
        NotificationRecord record = buildRecord(USER_ID, NotificationType.REMINDER);
        Page<NotificationRecord> page = new PageImpl<>(List.of(record));
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationRecordRepository.findByUserIdAndType(eq(USER_ID), eq(NotificationType.REMINDER), eq(pageable)))
                .thenReturn(page);

        // When
        Page<NotificationRecordResponse> result = service.getMyNotifications(USER_ID, "REMINDER", pageable, null);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(notificationRecordRepository).findByUserIdAndType(
                eq(USER_ID), eq(NotificationType.REMINDER), any(Pageable.class));
    }

    @Test
    @DisplayName("NOTIF-TC-011-003: getMyNotifications empty page — returns empty content")
    void getMyNotifications_noNotifications_returnsEmptyPage() {
        // Given
        when(notificationRecordRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        // When
        Page<NotificationRecordResponse> result =
                service.getMyNotifications(USER_ID, null, PageRequest.of(0, 20), null);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private NotificationRecord buildRecord(UUID userId, NotificationType type) {
        return NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .title("Test Notification")
                .body("Test body")
                .status(NotificationRecordStatus.SENT)
                .isRead(false)
                .build();
    }
}
