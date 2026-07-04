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
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.mapper.AdminUserMapper;
import com.carebridge.backend.identity.admin.service.impl.AdminUserServiceImpl;
import com.carebridge.backend.identity.admin.testsupport.AdminGovernanceTestFactory;
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

    private AdminUserServiceImpl newService() {
        return new AdminUserServiceImpl(userRepository, auditService, new AdminUserMapper());
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
        assertThat(savedCaptor.getValue().isEnabled()).isTrue(); // unchanged
    }

    // UC114-TC-005
    @Test
    void updateStatus_reEnablesAndUnlocksPreviouslyDisabledAccount() {
        AdminUserServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MOTHER, u -> {
            u.setEnabled(false);
            u.setLocked(true);
        });
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeStatusRequest(r -> {
                    r.setEnabled(true);
                    r.setLocked(false);
                }));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isEnabled()).isTrue();
        assertThat(savedCaptor.getValue().isLocked()).isFalse();
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
