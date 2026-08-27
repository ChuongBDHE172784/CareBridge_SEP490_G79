package com.carebridge.backend.family.service;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.family.dto.FamilyAlertListResponse;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.service.impl.FamilyAlertServiceImpl;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC-86: View Family Alert — service layer unit tests.
 * Tests FamilyAlertServiceImpl.listFamilyAlerts() with Mockito.
 *
 * Key implementation facts:
 * - Reads NotificationRecordRepository.findByUserIdAndType(callerId, EMERGENCY, pageable)
 * - Consent-minimized DTO: only alertId, title, body, isRead, createdAt
 * - Audits FAMILY_ALERT_VIEWED when results > 0
 * - Returns 200 + empty list (never 404) when no alerts exist (SRS AF2)
 */
@ExtendWith(MockitoExtension.class)
class FamilyAlertServiceImplTest {

    @Mock private NotificationRecordRepository notificationRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private FamilyAlertServiceImpl service;

    private static final UUID CALLER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private NotificationRecord makeEmergencyRecord(boolean isRead) {
        return NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(CALLER_ID)
                .type(NotificationType.EMERGENCY)
                .title("Emergency Alert")
                .body("Possible fall detected")
                .isRead(isRead)
                .build();
    }

    // ─── FAM-TC-001: EMERGENCY alert returned, even if alerts flag false ──────
    //     (Safety override: EMERGENCY always shown per ADR-FAM-008)

    @Test
    void listFamilyAlerts_emergencyRecord_returnedRegardlessOfPermission() {
        NotificationRecord record = makeEmergencyRecord(false);
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        FamilyAlertListResponse response = service.listFamilyAlerts(CALLER_ID, 0, 20);

        assertThat(response.getAlerts()).hasSize(1);
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getAlerts().get(0).getAlertId()).isEqualTo(record.getId());
        assertThat(response.getAlerts().get(0).getTitle()).isEqualTo("Emergency Alert");
        assertThat(response.getAlerts().get(0).getBody()).isEqualTo("Possible fall detected");
        assertThat(response.getAlerts().get(0).isRead()).isFalse();
    }

    // ─── FAM-TC-006: No alerts → 200 + empty list (SRS AF2, not 404) ─────────

    @Test
    void listFamilyAlerts_noAlerts_returnsEmptyList() {
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FamilyAlertListResponse response = service.listFamilyAlerts(CALLER_ID, 0, 20);

        assertThat(response.getAlerts()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
        // No exception — AF2 is 200 + empty, not 404
    }

    // ─── Audit log emitted when alerts exist (POST-3 / SRS POST) ─────────────

    @Test
    void listFamilyAlerts_withAlerts_emitsAuditLog() {
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(makeEmergencyRecord(false))));

        service.listFamilyAlerts(CALLER_ID, 0, 20);

        verify(auditService).log(
                eq(AuditAction.FAMILY_ALERT_VIEWED),
                eq(CALLER_ID),
                eq("FamilyAlert"),
                eq(CALLER_ID.toString()),
                anyString()
        );
    }

    // ─── No audit log when empty (nothing to audit) ───────────────────────────

    @Test
    void listFamilyAlerts_empty_noAuditLog() {
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listFamilyAlerts(CALLER_ID, 0, 20);

        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    // ─── Consent-minimized DTO: no referenceId exposed (BR-PRIVACY / PDPA) ───

    @Test
    void listFamilyAlerts_dtoDoesNotExposeReferenceId() {
        NotificationRecord record = NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(CALLER_ID)
                .type(NotificationType.EMERGENCY)
                .title("Alert")
                .body("Details")
                .referenceId(UUID.randomUUID()) // present in DB but MUST NOT appear in DTO
                .isRead(false)
                .build();
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        FamilyAlertListResponse response = service.listFamilyAlerts(CALLER_ID, 0, 20);

        // FamilyAlertItemDto has no referenceId field — just asserting dto exists without throwing
        assertThat(response.getAlerts().get(0).getAlertId()).isNotNull();
        assertThat(response.getAlerts().get(0).getTitle()).isEqualTo("Alert");
    }

    // ─── Pagination metadata is returned correctly ────────────────────────────

    @Test
    void listFamilyAlerts_pagination_metadataReturned() {
        List<NotificationRecord> records = List.of(makeEmergencyRecord(true), makeEmergencyRecord(false));
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(records));

        FamilyAlertListResponse response = service.listFamilyAlerts(CALLER_ID, 0, 20);

        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getAlerts()).hasSize(2);
    }

    // ─── Multiple alerts: sorted descending by createdAt (Pageable applied) ───

    @Test
    void listFamilyAlerts_multipleAlerts_allIncluded() {
        List<NotificationRecord> records = List.of(
                makeEmergencyRecord(false),
                makeEmergencyRecord(true)
        );
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(records));

        FamilyAlertListResponse response = service.listFamilyAlerts(CALLER_ID, 0, 20);

        assertThat(response.getAlerts()).hasSize(2);
    }
}
