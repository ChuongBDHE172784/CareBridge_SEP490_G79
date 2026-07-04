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
import com.carebridge.backend.identity.admin.dto.response.UserRoleResponse;
import com.carebridge.backend.identity.admin.mapper.UserRoleMapper;
import com.carebridge.backend.identity.admin.service.impl.AdminRoleServiceImpl;
import com.carebridge.backend.identity.admin.testsupport.AdminGovernanceTestFactory;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * UC116 Update Role and Permission — unit tests (UC116_UpdateRoleAndPermission_Test-Spec.md §4).
 * Covers TC-001, TC-002, TC-003, TC-004, TC-006, TC-007, TC-011, TC-012, plus the
 * release-blocking self-escalation gate (TC-SEC-001 equivalent, unit-level).
 */
@ExtendWith(MockitoExtension.class)
class AdminRoleServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private AdminRoleServiceImpl newService() {
        return new AdminRoleServiceImpl(userRepository, auditService, new UserRoleMapper());
    }

    // UC116-TC-001
    @Test
    void updateRole_happyPath_reassignsRoleAndAudits() {
        AdminRoleServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MODERATOR);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleResponse response = service.updateRole(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> r.setNewRole(Role.CONTENT_ADMIN)));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getRole()).isEqualTo(Role.CONTENT_ADMIN);
        assertThat(response.getPreviousRole()).isEqualTo(Role.MODERATOR);
        assertThat(response.getNewRole()).isEqualTo(Role.CONTENT_ADMIN);

        verify(auditService, times(1)).log(
                eq(AuditAction.ROLE_PERMISSION_UPDATED), eq(admin.getId()), eq("USER"),
                eq(target.getId().toString()), any());
    }

    // UC116-TC-002
    @Test
    void updateRole_reassignsRoleAndLocksAccessRightsAtomically() {
        AdminRoleServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MODERATOR, u -> u.setLocked(false));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateRole(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> {
                    r.setNewRole(Role.MODERATOR);
                    r.setLockAccessRights(true);
                }));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().isLocked()).isTrue();
        assertThat(savedCaptor.getValue().getRole()).isEqualTo(Role.MODERATOR);
    }

    // UC116-TC-003
    @Test
    void updateRole_promotesDifferentTargetToSystemAdmin_succeeds() {
        AdminRoleServiceImpl service = newService();
        User adminA = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MODERATOR);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleResponse response = service.updateRole(adminA.getId(), target.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> r.setNewRole(Role.SYSTEM_ADMIN)));

        assertThat(response.getNewRole()).isEqualTo(Role.SYSTEM_ADMIN);
        verify(auditService).log(eq(AuditAction.ROLE_PERMISSION_UPDATED), eq(adminA.getId()), eq("USER"),
                eq(target.getId().toString()), any());
    }

    // UC116-TC-004
    @Test
    void updateRole_auditPayloadAlwaysCapturesPreviousAndNewRole() {
        AdminRoleServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MODERATOR);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateRole(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> r.setNewRole(Role.CONTENT_ADMIN)));

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(auditService).log(eq(AuditAction.ROLE_PERMISSION_UPDATED), any(UUID.class), eq("USER"),
                any(String.class), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isNotNull();
        assertThat(payloadCaptor.getValue().toString()).contains("MODERATOR").contains("CONTENT_ADMIN");
    }

    // UC116-TC-006 — self-demotion (proves the guard is unconditional, not escalation-only)
    @Test
    void updateRole_selfDemotion_rejectedUnconditionally() {
        AdminRoleServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();

        assertThatThrownBy(() -> service.updateRole(admin.getId(), admin.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> r.setNewRole(Role.MOTHER))))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(auditService);
    }

    // UC116-TC-SEC-001 (unit-level) — self-escalation attempt, including the "same role" edge case
    @Test
    void updateRole_selfEscalationToSystemAdmin_rejectedEvenIfAlreadySystemAdmin() {
        AdminRoleServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();

        assertThatThrownBy(() -> service.updateRole(admin.getId(), admin.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> r.setNewRole(Role.SYSTEM_ADMIN))))
                .isInstanceOf(AccessDeniedBusinessException.class);

        verify(userRepository, never()).save(any());
    }

    // UC116-TC-007
    @Test
    void updateRole_targetNotFound_throwsResourceNotFound() {
        AdminRoleServiceImpl service = newService();
        UUID adminId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole(adminId, unknownId,
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> {})))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // UC116-TC-011 — no-op reassignment: TDS is silent, spec decision = allowed + audited
    @Test
    void updateRole_noOpReassignment_succeedsAndIsStillAudited() {
        AdminRoleServiceImpl service = newService();
        User admin = AdminGovernanceTestFactory.makeSystemAdmin();
        User target = AdminGovernanceTestFactory.makeUser(Role.MODERATOR);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRoleResponse response = service.updateRole(admin.getId(), target.getId(),
                AdminGovernanceTestFactory.makeUpdateRoleRequest(r -> r.setNewRole(Role.MODERATOR)));

        assertThat(response.getPreviousRole()).isEqualTo(Role.MODERATOR);
        assertThat(response.getNewRole()).isEqualTo(Role.MODERATOR);
        verify(auditService, times(1)).log(any(), any(UUID.class), any(), any(), any());
    }

    // UC116-TC-012 — no permission-flag-schema dependency
    @Test
    void service_hasNoPermissionFlagSchemaDependency() {
        boolean hasSuspiciousField = java.util.Arrays.stream(AdminRoleServiceImpl.class.getDeclaredFields())
                .anyMatch(f -> f.getType().getSimpleName().toLowerCase().contains("permission")
                        || f.getType().getSimpleName().toLowerCase().contains("userrolerepository"));
        assertThat(hasSuspiciousField).isFalse();
    }
}
