package com.carebridge.backend.identity.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.response.AdminUserActivityResponse;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSessionResponse;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.mapper.AdminUserMapper;
import com.carebridge.backend.identity.admin.repository.AdminUserMonitoringRepository;
import com.carebridge.backend.identity.admin.service.impl.AdminUserServiceImpl;
import com.carebridge.backend.identity.admin.testsupport.AdminGovernanceTestFactory;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * UC114 Manage User Accounts — unit tests (UC114_ManageUserAccounts_Test-Spec.md §4).
 * Covers TC-001, TC-003, TC-004, TC-005, TC-006, TC-007, TC-010, TC-012.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private AdminUserMonitoringRepository monitoringRepository;
    @Mock private UserSessionRepository userSessionRepository;

    private AdminUserServiceImpl newService() {
        return new AdminUserServiceImpl(
                userRepository,
                auditService,
                new AdminUserMapper(),
                monitoringRepository,
                userSessionRepository);
    }

    // UC114-TC-001
    @Test
    void searchUsers_delegatesFilterToRepositoryAndMapsFields() {
        AdminUserServiceImpl service = newService();
        User mother = AdminGovernanceTestFactory.makeUser(Role.MOTHER);
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.search(eq(null), eq(null), eq(null), eq(Role.MOTHER), eq(true), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(mother), pageable, 1));

        Page<AdminUserSummaryResponse> result = service.searchUsers(
                AdminGovernanceTestFactory.makeSearchQuery(q -> {
                    q.setRole(Role.MOTHER);
                    q.setEnabled(true);
                }), pageable);

        assertThat(result.getContent()).hasSize(1);
        AdminUserSummaryResponse dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(mother.getId());
        assertThat(dto.getEmail()).isEqualTo(mother.getEmail());
        assertThat(dto.getRole()).isEqualTo(Role.MOTHER);
    }

    // UC114-TC-015
    @Test
    void getUser_existingTarget_returnsSafeSummary() {
        AdminUserServiceImpl service = newService();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER);
        target.setPasswordHash("secret-hash");
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        AdminUserSummaryResponse result = service.getUser(target.getId());

        assertThat(result.getId()).isEqualTo(target.getId());
        assertThat(result.getEmail()).isEqualTo(target.getEmail());
        assertThat(result.getRole()).isEqualTo(target.getRole());
    }

    // UC114-TC-016
    @Test
    void getUser_unknownTarget_throwsResourceNotFound() {
        AdminUserServiceImpl service = newService();
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(targetId))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(monitoringRepository);
    }

    // UC114-TC-017 / TC-018
    @Test
    void getUserSessions_returnsPrivacyMinimizedProjection() {
        AdminUserServiceImpl service = newService();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER);
        Pageable pageable = PageRequest.of(0, 20);
        UserSession session = UserSession.builder()
                .sessionId(UUID.randomUUID())
                .userId(target.getId())
                .refreshTokenHash("must-not-leak")
                .deviceName("Chrome on macOS")
                .ipAddress("127.0.0.1")
                .status("ACTIVE")
                .createdAt(Instant.parse("2026-07-28T01:00:00Z"))
                .lastActivityAt(Instant.parse("2026-07-28T02:00:00Z"))
                .expiresAt(Instant.parse("2026-08-01T01:00:00Z"))
                .tokenFamilyId(UUID.randomUUID())
                .deviceIdentifier("sensitive-device-id")
                .build();
        when(userRepository.existsById(target.getId())).thenReturn(true);
        when(monitoringRepository.findSessions(target.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));

        Page<AdminUserSessionResponse> result = service.getUserSessions(target.getId(), pageable);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(session.getSessionId());
            assertThat(dto.getDeviceName()).isEqualTo("Chrome on macOS");
            assertThat(dto.getStatus()).isEqualTo("ACTIVE");
            assertThat(dto.getIssuedAt()).isEqualTo(session.getCreatedAt());
        });
    }

    // UC114-TC-019
    @Test
    void getUserActivity_returnsGovernanceTimeline() {
        AdminUserServiceImpl service = newService();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER);
        UUID actorId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        AuditLog log = AuditLog.builder()
                .auditLogId(UUID.randomUUID())
                .actorUserId(actorId)
                .action(AuditAction.ROLE_PERMISSION_UPDATED)
                .entityType("USER")
                .entityId(target.getId())
                .createdAt(Instant.parse("2026-07-28T02:00:00Z"))
                .newValueJson("{\"newRole\":\"EXPERT\"}")
                .build();
        when(userRepository.existsById(target.getId())).thenReturn(true);
        when(monitoringRepository.findActivity(target.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        Page<AdminUserActivityResponse> result = service.getUserActivity(target.getId(), pageable);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getActorUserId()).isEqualTo(actorId);
            assertThat(dto.getAction()).isEqualTo(AuditAction.ROLE_PERMISSION_UPDATED);
            assertThat(dto.getDetails()).contains("newRole");
        });
    }

    @Test
    void getUserMonitoring_unknownTarget_throwsBeforeQueryingMonitoringData() {
        AdminUserServiceImpl service = newService();
        UUID targetId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.existsById(targetId)).thenReturn(false);

        assertThatThrownBy(() -> service.getUserSessions(targetId, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(monitoringRepository);
    }

    // UC114-TC-003
    @Test
    void updateStatus_disablesTargetAndEmitsAudit() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER, u -> u.setEnabled(true));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserSummaryResponse response = service.updateStatus(
                admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> r.setEnabled(false)));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isEnabled()).isFalse();
        assertThat(response.isEnabled()).isFalse();
        verify(userSessionRepository).revokeAllByUserId(eq(target.getId()), any(Instant.class));

        verify(auditService, times(1)).log(
                eq(AuditAction.USER_ACCOUNT_STATUS_CHANGED), eq(admin.getId()), eq("USER"),
                eq(target.getId().toString()), any());
    }

    // UC114-TC-004
    @Test
    void updateStatus_locksTargetLeavingEnabledUnchanged() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER, u -> u.setLocked(false));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> {
                    r.setEnabled(null);
                    r.setLocked(true);
                }));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isLocked()).isTrue();
        assertThat(savedCaptor.getValue().getLockedAt()).isNotNull();
        assertThat(savedCaptor.getValue().getLockType()).isEqualTo(AccountLockType.ADMIN);
        assertThat(savedCaptor.getValue().getLockReason()).isEqualTo("Suspected policy violation — pending review");
        assertThat(savedCaptor.getValue().getLockedBy()).isEqualTo(admin.getId());
        assertThat(savedCaptor.getValue().getLockEpisodeId()).isNotNull();
        assertThat(savedCaptor.getValue().isEnabled()).isTrue(); // unchanged
        verify(userSessionRepository).revokeAllByUserId(eq(target.getId()), any(Instant.class));
    }

    // UC114-TC-005
    @Test
    void updateStatus_reEnablesAndUnlocksPreviouslyDisabledAccount() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        UUID lockEpisodeId = UUID.randomUUID();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER, u -> {
            u.setEnabled(false);
            u.setLocked(true);
            u.setLockedAt(Instant.now());
            u.setLockType(AccountLockType.ADMIN);
            u.setLockReason("Administrative review");
            u.setLockedBy(admin.getId());
            u.setLockEpisodeId(lockEpisodeId);
        });
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> {
                    r.setEnabled(true);
                    r.setLocked(false);
                    r.setReason("Verified by customer support");
                    r.setCskhTicketId("CSKH-2026-0042");
                }));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isEnabled()).isTrue();
        assertThat(savedCaptor.getValue().isLocked()).isFalse();
        assertThat(savedCaptor.getValue().getLockedAt()).isNull();
        assertThat(savedCaptor.getValue().getLockType()).isNull();
        assertThat(savedCaptor.getValue().getLockReason()).isNull();
        assertThat(savedCaptor.getValue().getLockedBy()).isNull();
        assertThat(savedCaptor.getValue().getLockEpisodeId()).isNull();

        // The appeal table is gone, so this audit row is the only lasting record of
        // the unlock: it must still name the episode, the reason and the support
        // ticket that authorised it.
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(
                eq(AuditAction.USER_ACCOUNT_STATUS_CHANGED), eq(admin.getId()), eq("USER"),
                eq(target.getId().toString()), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().toString())
                .contains(lockEpisodeId.toString())
                .contains("CSKH-2026-0042")
                .contains("Verified by customer support");
    }

    @Test
    void updateStatus_lockWithoutReason_isRejected() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MODERATOR);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateStatus(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> {
                    r.setEnabled(null);
                    r.setLocked(true);
                    r.setReason("   ");
                })))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lock reason is required");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(userSessionRepository);
    }

    // UC114-TC-006
    @Test
    void updateStatus_rejectsSelfTargetDisable() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();

        assertThatThrownBy(() -> service.updateStatus(admin.getId(), admin.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> r.setEnabled(false))))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(auditService);
    }

    // UC114-TC-007
    @Test
    void updateStatus_rejectsSelfTargetLock() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();

        assertThatThrownBy(() -> service.updateStatus(admin.getId(), admin.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> {
                    r.setEnabled(null);
                    r.setLocked(true);
                })))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verify(userRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(UUID.class), any(), any(), any());
    }

    // UC114-TC-010
    @Test
    void updateStatus_targetNotFound_throwsResourceNotFound() {
        AdminUserServiceImpl service = newService();
        UUID adminId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(adminId, unknownId,
                AdminGovernanceTestFactory.makeStatusRequest(r -> r.setEnabled(false))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // UC114-TC-012
    @Test
    void updateStatus_bothFieldsOmitted_rejectedAsNoOp() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        UUID targetId = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateStatus(admin.getId(), targetId,
                AdminGovernanceTestFactory.makeStatusRequest(r -> {
                    r.setEnabled(null);
                    r.setLocked(null);
                })))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(auditService);
    }
}
