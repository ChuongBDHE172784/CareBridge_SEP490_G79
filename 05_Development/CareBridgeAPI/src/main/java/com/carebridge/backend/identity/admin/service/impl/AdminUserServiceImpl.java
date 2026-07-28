package com.carebridge.backend.identity.admin.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.AdminUserSearchQuery;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.response.AdminUserActivityResponse;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSessionResponse;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.mapper.AdminUserMapper;
import com.carebridge.backend.identity.admin.repository.AccountLockAppealRepository;
import com.carebridge.backend.identity.admin.repository.AdminUserMonitoringRepository;
import com.carebridge.backend.identity.admin.service.AdminUserService;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.entity.AccountLockType;
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
    private final AdminUserMonitoringRepository monitoringRepository;
    private final UserSessionRepository userSessionRepository;
    private final AccountLockAppealRepository appealRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponse> searchUsers(AdminUserSearchQuery query, Pageable pageable) {
        return userRepository.search(
                        query.getEmail(), query.getPhone(), query.getName(),
                        query.getRole(), query.getEnabled(), query.getLocked(), pageable)
                .map(adminUserMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserSummaryResponse getUser(UUID userId) {
        return userRepository.findById(userId)
                .map(adminUserMapper::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("IAM-114-003: User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSessionResponse> getUserSessions(UUID userId, Pageable pageable) {
        requireUserExists(userId);
        return monitoringRepository.findSessions(userId, pageable)
                .map(session -> AdminUserSessionResponse.builder()
                        .id(session.getSessionId())
                        .deviceName(session.getDeviceName())
                        .status(session.getStatus())
                        .issuedAt(session.getCreatedAt())
                        .lastActivityAt(session.getLastActivityAt())
                        .expiresAt(session.getExpiresAt())
                        .revokedAt(session.getRevokedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserActivityResponse> getUserActivity(UUID userId, Pageable pageable) {
        requireUserExists(userId);
        return monitoringRepository.findActivity(userId, pageable)
                .map(log -> AdminUserActivityResponse.builder()
                        .id(log.getAuditLogId())
                        .actorUserId(log.getActorUserId())
                        .action(log.getAction())
                        .timestamp(log.getCreatedAt())
                        .details(log.getNewValueJson())
                        .build());
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

        if (Boolean.TRUE.equals(request.getLocked())
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new ValidationException("IAM-114-005: lock reason is required");
        }

        boolean previousEnabled = target.isEnabled();
        boolean previousLocked = target.isLocked();

        Instant now = Instant.now();
        if (request.getEnabled() != null) {
            target.setEnabled(request.getEnabled());
            if (!request.getEnabled()) userSessionRepository.revokeAllByUserId(targetUserId, now);
        }
        if (request.getLocked() != null) {
            if (request.getLocked()) {
                target.setLocked(true);
                target.setLockedAt(now);
                target.setLockType(AccountLockType.ADMIN);
                target.setLockReason(request.getReason().trim());
                target.setLockedBy(callerUserId);
                target.setLockEpisodeId(UUID.randomUUID());
                userSessionRepository.revokeAllByUserId(targetUserId, now);
            } else {
                UUID episodeId = target.getLockEpisodeId();
                target.setLocked(false);
                target.setLockedAt(null);
                target.setLockType(null);
                target.setLockReason(null);
                target.setLockedBy(null);
                target.setLockEpisodeId(null);
                if (episodeId != null) {
                    appealRepository.cancelPending(targetUserId, episodeId, now, callerUserId,
                            "Closed because System Admin unlocked the account directly");
                }
            }
        }

        User saved = userRepository.save(target);

        auditService.log(AuditAction.USER_ACCOUNT_STATUS_CHANGED, callerUserId, "USER", targetUserId.toString(),
                new UserAccountStatusChangedPayload(
                        targetUserId, previousEnabled, saved.isEnabled(),
                        previousLocked, saved.isLocked(), request.getReason()));

        return adminUserMapper.toSummary(saved);
    }

    private void requireUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("IAM-114-003: User not found");
        }
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
