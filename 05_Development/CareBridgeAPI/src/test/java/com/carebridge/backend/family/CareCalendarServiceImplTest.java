package com.carebridge.backend.family;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.SharedCareCalendarResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.impl.CareCalendarServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.carebridge.backend.family.CareCalendarTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CareCalendarServiceImpl — UC-74 View Shared Care Calendar.
 *
 * Tests: FAM-UC74-TC-001 through TC-007, TC-014, TC-015.
 * Policy tests (TC-008, TC-009) are in CareGroupAccessPolicyTest.
 * Repository tests (TC-010, TC-011) are in CareTaskRepositoryIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class CareCalendarServiceImplTest {

    @Mock private CareGroupRepository groupRepository;
    @Mock private CareGroupMemberRepository memberRepository;
    @Mock private CareTaskRepository taskRepository;
    @Mock private CareGroupAuthorizationPolicy authorizationPolicy;
    @InjectMocks private CareCalendarServiceImpl service;

    // ── Helper: stub happy-path for ACC_001 (ACCEPTED member + calendar=true) ──

    private void stubHappyPath(UUID groupId, UUID callerId) {
        CareGroup group = makeGroup(groupId);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(authorizationPolicy.isMember(groupId, callerId)).thenReturn(true);
        when(authorizationPolicy.isOwner(groupId, callerId)).thenReturn(false);
        when(authorizationPolicy.hasPermission(groupId, callerId, PermissionFlag.CALENDAR)).thenReturn(true);
    }

    // ── FAM-UC74-TC-001: ACCEPTED + calendar=true → sees tasks ───────────────

    @Test
    void getCalendar_acceptedMemberWithCalendarTrue_returnsTasksInRange() {
        stubHappyPath(GROUP_CG_001, ACC_001);
        CareTask taskInRange = makeTask(GROUP_CG_001, JULY_TASK_DUE_AT);
        when(taskRepository.findByCareGroupIdAndDueAtBetween(GROUP_CG_001, RANGE_START, RANGE_END))
                .thenReturn(List.of(taskInRange));

        SharedCareCalendarResponse response = service.getCalendar(GROUP_CG_001, ACC_001, RANGE_START, RANGE_END);

        assertThat(response).isNotNull();
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTaskId()).isEqualTo(taskInRange.getId());
    }

    // ── FAM-UC74-TC-002: ACCEPTED + calendar=false → 403 FAM-007 ─────────────

    @Test
    void getCalendar_acceptedMemberCalendarFalse_throws403Fam007() {
        CareGroup group = makeGroup(GROUP_CG_001);
        when(groupRepository.findById(GROUP_CG_001)).thenReturn(Optional.of(group));
        when(authorizationPolicy.isMember(GROUP_CG_001, ACC_002)).thenReturn(true);
        when(authorizationPolicy.isOwner(GROUP_CG_001, ACC_002)).thenReturn(false);
        when(authorizationPolicy.hasPermission(GROUP_CG_001, ACC_002, PermissionFlag.CALENDAR)).thenReturn(false);

        assertThatThrownBy(() -> service.getCalendar(GROUP_CG_001, ACC_002, RANGE_START, RANGE_END))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("FAM-007");
                });
        verify(taskRepository, never()).findByCareGroupIdAndDueAtBetween(any(), any(), any());
    }

    // ── FAM-UC74-TC-003: PENDING member → 403 FAM-003 ────────────────────────

    @Test
    void getCalendar_pendingMember_throws403Fam003() {
        CareGroup group = makeGroup(GROUP_CG_001);
        when(groupRepository.findById(GROUP_CG_001)).thenReturn(Optional.of(group));
        when(authorizationPolicy.isMember(GROUP_CG_001, ACC_003)).thenReturn(false); // PENDING → not ACCEPTED

        assertThatThrownBy(() -> service.getCalendar(GROUP_CG_001, ACC_003, RANGE_START, RANGE_END))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-003"));
        // hasPermission must NOT be called when isMember returns false (C1: two-step check)
        verify(authorizationPolicy, never()).hasPermission(any(), any(), any());
    }

    // ── FAM-UC74-TC-004: REVOKED member → 403 FAM-003 ───────────────────────

    @Test
    void getCalendar_revokedMember_throws403Fam003() {
        CareGroup group = makeGroup(GROUP_CG_001);
        when(groupRepository.findById(GROUP_CG_001)).thenReturn(Optional.of(group));
        when(authorizationPolicy.isMember(GROUP_CG_001, ACC_004)).thenReturn(false); // REVOKED → not ACCEPTED

        assertThatThrownBy(() -> service.getCalendar(GROUP_CG_001, ACC_004, RANGE_START, RANGE_END))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-003"));
    }

    // ── FAM-UC74-TC-005: Non-member → 403 FAM-003 ────────────────────────────

    @Test
    void getCalendar_nonMember_throws403Fam003() {
        CareGroup group = makeGroup(GROUP_CG_001);
        when(groupRepository.findById(GROUP_CG_001)).thenReturn(Optional.of(group));
        when(authorizationPolicy.isMember(GROUP_CG_001, ACC_005)).thenReturn(false);

        assertThatThrownBy(() -> service.getCalendar(GROUP_CG_001, ACC_005, RANGE_START, RANGE_END))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-003"));
    }

    // ── FAM-UC74-TC-006: No tasks in range → 200 empty list (AF2) ────────────

    @Test
    void getCalendar_noTasksInRange_returns200WithEmptyList() {
        Instant augustStart = Instant.parse("2026-08-01T00:00:00Z");
        Instant augustEnd   = Instant.parse("2026-08-31T23:59:59Z");

        stubHappyPath(GROUP_CG_001, ACC_001);
        when(taskRepository.findByCareGroupIdAndDueAtBetween(GROUP_CG_001, augustStart, augustEnd))
                .thenReturn(List.of());

        SharedCareCalendarResponse response = service.getCalendar(GROUP_CG_001, ACC_001, augustStart, augustEnd);

        assertThat(response).isNotNull();
        assertThat(response.getTotalItems()).isZero();
        assertThat(response.getItems()).isNotNull().isEmpty(); // not null — AF2 returns [] not 404
    }

    // ── FAM-UC74-TC-007: Group not found → 404 FAM-005 ───────────────────────

    @Test
    void getCalendar_groupNotFound_throws404Fam005() {
        UUID nonExistentGroupId = UUID.fromString("00000000-0000-0000-0000-000000009999");
        when(groupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCalendar(nonExistentGroupId, ACC_001, RANGE_START, RANGE_END))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("FAM-005");
                });
    }

    // ── FAM-UC74-TC-014: Cross-group access attempt → 403 FAM-003 ────────────

    @Test
    void getCalendar_crossGroupAccessAttempt_throws403Fam003() {
        // ACC_001 is member of GROUP_CG_001 only, tries to access GROUP_CG_002
        CareGroup group2 = makeGroup(GROUP_CG_002);
        when(groupRepository.findById(GROUP_CG_002)).thenReturn(Optional.of(group2));
        when(authorizationPolicy.isMember(GROUP_CG_002, ACC_001)).thenReturn(false); // not a member of CG_002

        assertThatThrownBy(() -> service.getCalendar(GROUP_CG_002, ACC_001, RANGE_START, RANGE_END))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("FAM-003"));
    }

    // ── FAM-UC74-TC-015: Response contains only care_tasks, no reminders ─────

    @Test
    void getCalendar_noReminderDataInResponse_onlyCareTasksReturned() {
        stubHappyPath(GROUP_CG_001, ACC_001);
        CareTask task = makeTask(GROUP_CG_001, JULY_TASK_DUE_AT);
        when(taskRepository.findByCareGroupIdAndDueAtBetween(GROUP_CG_001, RANGE_START, RANGE_END))
                .thenReturn(List.of(task));

        SharedCareCalendarResponse response = service.getCalendar(GROUP_CG_001, ACC_001, RANGE_START, RANGE_END);

        // All items must have a taskId (from care_tasks table); no reminder-specific fields
        assertThat(response.getItems()).allSatisfy(item -> {
            assertThat(item.getTaskId()).isNotNull();
            assertThat(item.getTitle()).isNotBlank();
        });
        // Confirm only 1 item from the 1 seeded task — no bonus items from another source
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    // ── OWNER bypasses permission check ───────────────────────────────────────

    @Test
    void getCalendar_owner_seesAllTasksWithoutPermissionCheck() {
        CareGroup group = makeGroup(GROUP_CG_001);
        when(groupRepository.findById(GROUP_CG_001)).thenReturn(Optional.of(group));
        when(authorizationPolicy.isMember(GROUP_CG_001, ACC_001)).thenReturn(true);
        when(authorizationPolicy.isOwner(GROUP_CG_001, ACC_001)).thenReturn(true); // OWNER
        CareTask task = makeTask(GROUP_CG_001, JULY_TASK_DUE_AT);
        when(taskRepository.findByCareGroupIdAndDueAtBetween(GROUP_CG_001, RANGE_START, RANGE_END))
                .thenReturn(List.of(task));

        SharedCareCalendarResponse response = service.getCalendar(GROUP_CG_001, ACC_001, RANGE_START, RANGE_END);

        assertThat(response.getTotalItems()).isEqualTo(1);
        // hasPermission NOT called for OWNER (ADR-FAM-003: OWNER sees all)
        verify(authorizationPolicy, never()).hasPermission(any(), any(), any());
    }
}
