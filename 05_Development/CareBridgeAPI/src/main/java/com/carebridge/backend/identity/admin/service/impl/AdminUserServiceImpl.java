package com.carebridge.backend.identity.admin.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.AdminUserSearchQuery;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.mapper.AdminUserMapper;
import com.carebridge.backend.identity.admin.service.AdminUserService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC114 Manage User Accounts — service implementation.
 * ADR-IAM-001: reads/writes only existing users.role/enabled/locked columns.
 * ADR-IAM-002: every status mutation audited in the same transaction.
 * ADR-IAM-003: self-target guard — admin cannot change their own account status.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AdminUserMapper adminUserMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> searchUsers(AdminUserSearchQuery query, Pageable pageable) {
        return userRepository.search(
                        query.getEmail(), query.getPhone(), query.getName(),
                        query.getRole(), query.getEnabled(), query.getLocked(), pageable)
                .map(adminUserMapper::toSummary);
    }

    @Override
    public AdminUserSummaryResponse updateStatus(UUID callerUserId, UUID targetUserId, UpdateUserStatusRequest request) {
        if (targetUserId.equals(callerUserId)) {
            throw new AccessDeniedBusinessException("IAM-114-004: Admin cannot change their own account status");
        }
        if (request.getEnabled() == null && request.getLocked() == null) {
            throw new ValidationException("IAM-114-002: at least one of enabled/locked must be provided");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("IAM-114-003: User not found"));

        boolean previousEnabled = target.isEnabled();
        boolean previousLocked = target.isLocked();

        if (request.getEnabled() != null) {
            target.setEnabled(request.getEnabled());
        }
        if (request.getLocked() != null) {
            target.setLocked(request.getLocked());
            target.setLockedAt(request.getLocked() ? Instant.now() : null);
        }

        User saved = userRepository.save(target);

        auditService.log(AuditAction.USER_ACCOUNT_STATUS_CHANGED, callerUserId, "USER", targetUserId.toString(),
                new UserAccountStatusChangedPayload(
                        targetUserId, previousEnabled, saved.isEnabled(),
                        previousLocked, saved.isLocked(), request.getReason()));

        return adminUserMapper.toSummary(saved);
    }

    private record UserAccountStatusChangedPayload(
            UUID targetUserId,
            Boolean previousEnabled,
            Boolean newEnabled,
            Boolean previousLocked,
            Boolean newLocked,
            String reason) {
    }
}
