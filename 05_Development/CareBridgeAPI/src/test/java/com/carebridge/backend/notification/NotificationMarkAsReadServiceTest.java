package com.carebridge.backend.notification;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UC-12 Mark Notifications As Read.
 *
 * TDD Test Spec: CB-NOTIF-IMP-012-TEST
 * Constraints enforced:
 *   C1 – ownership check before mutation
 *   C2 – atomic update of is_read + read_at
 *   C3 – idempotent (already-read → 200, no error)
 *   C4 – audit only when affected > 0
 *   C5 – server-side Instant.now() (not client-provided)
 */
@ExtendWith(MockitoExtension.class)
class NotificationMarkAsReadServiceTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private NotificationServiceImpl service;

    private static final UUID USER_ID       = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID NOTIF_ID      = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000012");

    // =========================================================================
    // NOTIF-TC-012-001: Mark single — happy path (unread → read)
    // =========================================================================

    @Test
    @DisplayName("NOTIF-TC-012-001: markAsRead unread notification → audit emitted once")
    void markAsRead_unreadNotification_emitsAudit() {
        // Given — notification exists, owned by USER_ID, is unread
        NotificationRecord unread = buildRecord(NOTIF_ID, USER_ID, false);
        when(notificationRecordRepository.findByIdAndUserId(NOTIF_ID, USER_ID))
                .thenReturn(Optional.of(unread));
        when(notificationRecordRepository.markAsReadById(eq(NOTIF_ID), eq(USER_ID), any(Instant.class)))
                .thenReturn(1);

        // When
        service.markAsRead(USER_ID, NOTIF_ID);

        // Then — markAsReadById called atomically with server Instant (C2, C5)
        verify(notificationRecordRepository).markAsReadById(eq(NOTIF_ID), eq(USER_ID), any(Instant.class));

        // C4 — audit emitted because affected = 1
        verify(auditService).log(
                eq(AuditAction.NOTIFICATIONS_READ),
                eq(USER_ID),
                eq("NotificationRecord"),
                eq(NOTIF_ID.toString()),
                any());
    }

    // =========================================================================
    // NOTIF-TC-012-002: Idempotent — already read → 200, no audit noise (C3, C4)
    // =========================================================================

    @Test
    @DisplayName("NOTIF-TC-012-002: markAsRead already-read notification → no exception, no audit")
    void markAsRead_alreadyRead_idempotentNoAudit() {
        // Given — notification is already read
        NotificationRecord alreadyRead = buildRecord(NOTIF_ID, USER_ID, true);
        when(notificationRecordRepository.findByIdAndUserId(NOTIF_ID, USER_ID))
                .thenReturn(Optional.of(alreadyRead));
        // markAsReadById returns 0 because WHERE is_read=false excludes it
        when(notificationRecordRepository.markAsReadById(eq(NOTIF_ID), eq(USER_ID), any(Instant.class)))
                .thenReturn(0);

        // When — must not throw
        service.markAsRead(USER_ID, NOTIF_ID);

        // Then — C4: no audit when affected = 0
        verify(auditService, never()).log(
                eq(AuditAction.NOTIFICATIONS_READ), any(), any(), any(), any());
    }

    // =========================================================================
    // NOTIF-TC-012-003: Ownership — wrong user → 404 (C1)
    // =========================================================================

    @Test
    @DisplayName("NOTIF-TC-012-003: markAsRead with wrong owner → ResourceNotFoundException")
    void markAsRead_wrongOwner_throwsNotFound() {
        // Given — repository returns empty because user_id doesn't match
        when(notificationRecordRepository.findByIdAndUserId(NOTIF_ID, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        // When / Then — C1: ownership check before mutation
        assertThatThrownBy(() -> service.markAsRead(OTHER_USER_ID, NOTIF_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        // No UPDATE should have been attempted
        verify(notificationRecordRepository, never())
                .markAsReadById(any(), any(), any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // NOTIF-TC-012-005: Mark all — happy path (3 unread → all marked)
    // =========================================================================

    @Test
    @DisplayName("NOTIF-TC-012-005: markAllAsRead with 3 unread → returns 3, audit emitted")
    void markAllAsRead_threeUnread_returnsCountAndEmitsAudit() {
        // Given
        when(notificationRecordRepository.markAllAsReadByUserId(eq(USER_ID), any(Instant.class)))
                .thenReturn(3);

        // When
        int result = service.markAllAsRead(USER_ID);

        // Then
        assertThat(result).isEqualTo(3);
        verify(auditService).log(
                eq(AuditAction.NOTIFICATIONS_READ),
                eq(USER_ID),
                eq("NotificationRecord"),
                eq(USER_ID.toString()),
                any());
    }

    @Test
    @DisplayName("NOTIF-TC-012-005b: markAllAsRead with nothing unread → returns 0, no audit")
    void markAllAsRead_nothingUnread_returnsZeroNoAudit() {
        when(notificationRecordRepository.markAllAsReadByUserId(eq(USER_ID), any(Instant.class)))
                .thenReturn(0);

        int result = service.markAllAsRead(USER_ID);

        assertThat(result).isEqualTo(0);
        // C4 — no audit when nothing actually changed
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private NotificationRecord buildRecord(UUID id, UUID userId, boolean isRead) {
        return NotificationRecord.builder()
                .id(id)
                .userId(userId)
                .type(NotificationType.REMINDER)
                .title("Test notification")
                .body("Test body")
                .status(NotificationRecordStatus.SENT)
                .isRead(isRead)
                .readAt(isRead ? Instant.now().minusSeconds(3600) : null)
                .build();
    }
}
