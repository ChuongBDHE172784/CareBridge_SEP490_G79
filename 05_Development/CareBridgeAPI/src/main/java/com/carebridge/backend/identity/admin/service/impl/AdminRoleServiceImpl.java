package com.carebridge.backend.identity.admin.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserRoleRequest;
import com.carebridge.backend.identity.admin.dto.response.UserRoleResponse;
import com.carebridge.backend.identity.admin.mapper.UserRoleMapper;
import com.carebridge.backend.identity.admin.service.AdminRoleService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC116 Update Role and Permission — service implementation.
 * ADR-IAM-007: self-target guard is UNCONDITIONAL — targetUserId == callerUserId is
 * always rejected regardless of role direction (stronger than UC114's guard).
 * ADR-IAM-008: every mutation synchronously audited, capturing both previousRole and
 * newRole, distinct AuditAction from UC114's status changes.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminRoleServiceImpl implements AdminRoleService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final UserRoleMapper userRoleMapper;

    @Override
    public UserRoleResponse updateRole(UUID callerUserId, UUID targetUserId, UpdateUserRoleRequest request) {
        assertNotSelfTarget(callerUserId, targetUserId);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("IAM-116-003: User not found"));

        Role previousRole = target.getRole();
        Boolean previousLocked = target.isLocked();

        target.setRole(request.getNewRole());
        if (request.getLockAccessRights() != null) {
            target.setLocked(request.getLockAccessRights());
        }

        User saved = userRepository.save(target);

        auditService.log(AuditAction.ROLE_PERMISSION_UPDATED, callerUserId, "USER", targetUserId.toString(),
                new RolePermissionUpdatedPayload(
                        targetUserId, previousRole, saved.getRole(), previousLocked, saved.isLocked(), request.getReason()));

        return userRoleMapper.toResponse(saved, previousRole);
    }

    private void assertNotSelfTarget(UUID callerUserId, UUID targetUserId) {
        if (targetUserId.equals(callerUserId)) {
            throw new AccessDeniedBusinessException("IAM-116-004: Admin cannot change their own role");
        }
    }

    private record RolePermissionUpdatedPayload(
            UUID targetUserId,
            Role previousRole,
            Role newRole,
            Boolean previousLocked,
            Boolean newLocked,
            String reason) {
    }
}
