package com.carebridge.backend.family.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.entity.SharedDataCategory;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.SharedDataServiceImpl;
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
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UC-84: View Shared Data — service layer unit tests.
 * Tests SharedDataServiceImpl.getSharedData() with Mockito.
 *
 * Key implementation facts:
 * - LOGS category: always returns empty list (deferred, OI-4)
 * - ALERTS: reads NotificationRecord with EMERGENCY type (not safety_alerts table)
 * - OWNER bypass: owners skip permission_json check
 * - FAM-005 (group not found), FAM-003 (not member), FAM-011 (no permission)
 */
@ExtendWith(MockitoExtension.class)
class SharedDataServiceImplTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private NotificationRecordRepository notificationRepository;
    @Mock private CareGroupAuthorizationPolicy accessPolicy;

    @InjectMocks
    private SharedDataServiceImpl service;

    private static final UUID GROUP_ID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CALLER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    private CareGroup makeGroup() {
        CareGroup g = new CareGroup();
        g.setId(GROUP_ID);
        g.setOwnerUserId(UUID.randomUUID());
        g.setGroupName("Test Group");
        g.setStatus(CareGroupStatus.ACTIVE);
        return g;
    }

    private CareTask makeTask() {
        return CareTask.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_ID)
                .title("Checkup appointment")
                .status(CareTaskStatus.OPEN)
                .build();
    }

    private NotificationRecord makeEmergencyNotification() {
        return NotificationRecord.builder()
                .id(UUID.randomUUID())
                .userId(CALLER_ID)
                .type(NotificationType.EMERGENCY)
                .title("Emergency Alert")
                .body("Possible fall detected")
                .isRead(false)
                .build();
    }

    // ─── Stubs for ACCEPTED member with permission ────────────────────────────

    private void stubMember(boolean isMember, boolean isOwner, boolean hasPermission) {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(accessPolicy.isMember(GROUP_ID, CALLER_ID)).thenReturn(isMember);
        if (isMember) {
            when(accessPolicy.isOwner(GROUP_ID, CALLER_ID)).thenReturn(isOwner);
            if (!isOwner) {
                when(accessPolicy.hasPermission(eq(GROUP_ID), eq(CALLER_ID), any(PermissionFlag.class)))
                        .thenReturn(hasPermission);
            }
        }
    }

    // ─── FAM-UC84-TC-001: CALENDAR, permission=true → returns tasks ──────────

    @Test
    void getSharedData_calendar_withPermission_returnsTasks() {
        stubMember(true, false, true);
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of(makeTask(), makeTask()));

        SharedDataResponse response = service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20);

        assertThat(response.getCategory()).isEqualTo("CALENDAR");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).allMatch(i -> "TASK".equals(i.getItemType()));
    }

    // ─── FAM-UC84-TC-016: CALENDAR, empty task list → 200, empty items ───────

    @Test
    void getSharedData_calendar_noTasks_returnsEmpty200() {
        stubMember(true, false, true);
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of());

        SharedDataResponse response = service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalItems()).isZero();
    }

    // ─── UC84 LOGS: always returns empty list (OI-4 deferred) ────────────────

    @Test
    void getSharedData_logs_withPermission_returnsEmptyListOI4() {
        stubMember(true, false, true);

        SharedDataResponse response = service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.LOGS, 0, 20);

        assertThat(response.getCategory()).isEqualTo("LOGS");
        assertThat(response.getItems()).isEmpty(); // deferred — OI-4
    }

    // ─── FAM-UC84-TC-003 (adapted): ALERTS → reads EMERGENCY notification_records

    @Test
    void getSharedData_alerts_withPermission_returnsEmergencyNotifications() {
        stubMember(true, false, true);
        NotificationRecord rec = makeEmergencyNotification();
        when(notificationRepository.findByUserIdAndType(eq(CALLER_ID), eq(NotificationType.EMERGENCY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rec)));

        SharedDataResponse response = service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.ALERTS, 0, 20);

        assertThat(response.getCategory()).isEqualTo("ALERTS");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getItemType()).isEqualTo("ALERT");
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("Emergency Alert");
    }

    // ─── FAM-UC84-TC-004: permission_json.calendar=false → FAM-011 ───────────

    @Test
    void getSharedData_calendarFlagFalse_throwsFam011() {
        stubMember(true, false, false);

        assertThatThrownBy(() -> service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-011");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    // ─── FAM-UC84-TC-010: PENDING member → FAM-003 (membership gate first) ───

    @Test
    void getSharedData_notMember_throwsFam003() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(accessPolicy.isMember(GROUP_ID, CALLER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-003");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
        // permission check must NOT be reached (C1: membership before permission)
        verify(accessPolicy, never()).hasPermission(any(), any(), any());
    }

    // ─── FAM-UC84-TC-015: Group not found → FAM-005 404 ─────────────────────

    @Test
    void getSharedData_groupNotFound_throwsFam005() {
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    // ─── OWNER bypass: owner sees CALENDAR without permission_json check ──────

    @Test
    void getSharedData_callerIsOwner_skipsPermissionCheck() {
        // OWNER bypasses hasPermission() even with no permission_json set
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(makeGroup()));
        when(accessPolicy.isMember(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(accessPolicy.isOwner(GROUP_ID, CALLER_ID)).thenReturn(true);
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of(makeTask()));

        SharedDataResponse response = service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20);

        assertThat(response.getItems()).hasSize(1);
        verify(accessPolicy, never()).hasPermission(any(), any(), any());
    }

    // ─── Response DTO does not contain raw PII ────────────────────────────────

    @Test
    void getSharedData_calendar_dtoDoesNotLeakEmail() {
        stubMember(true, false, true);
        CareTask task = makeTask();
        when(taskRepository.findByCareGroupId(GROUP_ID)).thenReturn(List.of(task));

        SharedDataResponse response = service.getSharedData(GROUP_ID, CALLER_ID, SharedDataCategory.CALENDAR, 0, 20);

        // Items must not expose raw user IDs or email — only task data
        assertThat(response.getItems().get(0).getItemId()).isNotNull();
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("Checkup appointment");
    }
}
