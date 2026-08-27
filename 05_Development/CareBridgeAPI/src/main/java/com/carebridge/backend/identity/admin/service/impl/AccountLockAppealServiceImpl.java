package com.carebridge.backend.identity.admin.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.ReviewAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.request.SubmitAccountLockAppealRequest;
import com.carebridge.backend.identity.admin.dto.response.AccountLockAppealResponse;
import com.carebridge.backend.identity.admin.entity.AccountLockAppeal;
import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import com.carebridge.backend.identity.admin.repository.AccountLockAppealRepository;
import com.carebridge.backend.identity.admin.service.AccountLockAppealService;
import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountLockAppealServiceImpl implements AccountLockAppealService {
    private final AccountLockAppealRepository appealRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    @Override
    public AccountLockAppealResponse submit(SubmitAccountLockAppealRequest request) {
        JwtTokenProvider.AppealTokenClaims claims = jwtTokenProvider.validateAppealToken(request.appealToken());
        if (claims == null) throw new ValidationException("IAM-114-APPEAL-001: Invalid or expired appeal token");

        User user = userRepository.findByIdForUpdate(claims.userId())
                .orElseThrow(() -> new ResourceNotFoundException("IAM-114-003: User not found"));
        if (!user.isLocked() || user.getLockType() != AccountLockType.ADMIN
                || !claims.lockEpisodeId().equals(user.getLockEpisodeId())) {
            throw new ValidationException("IAM-114-APPEAL-002: Lock episode is no longer active");
        }
        if (appealRepository.existsByUserIdAndLockEpisodeId(user.getId(), user.getLockEpisodeId())) {
            throw new ValidationException("IAM-114-APPEAL-003: An appeal already exists for this lock episode");
        }

        AccountLockAppeal appeal = appealRepository.save(AccountLockAppeal.builder()
                .userId(user.getId())
                .lockEpisodeId(user.getLockEpisodeId())
                .reason(request.reason().trim())
                .status(AccountLockAppealStatus.PENDING)
                .submittedAt(Instant.now())
                .build());
        auditService.log(AuditAction.ACCOUNT_LOCK_APPEAL_SUBMITTED, user.getId(),
                "ACCOUNT_LOCK_APPEAL", appeal.getId().toString(),
                Map.of("lockEpisodeId", appeal.getLockEpisodeId()));
        return map(appeal, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountLockAppealResponse> list(AccountLockAppealStatus status, Pageable pageable) {
        AccountLockAppealStatus effective = status == null ? AccountLockAppealStatus.PENDING : status;
        return appealRepository.findByStatusOrderBySubmittedAtDesc(effective, pageable)
                .map(appeal -> map(appeal, userRepository.findById(appeal.getUserId()).orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountLockAppealResponse get(UUID appealId) {
        AccountLockAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("IAM-114-APPEAL-004: Appeal not found"));
        return map(appeal, userRepository.findById(appeal.getUserId()).orElse(null));
    }

    @Override
    public AccountLockAppealResponse review(
            UUID reviewerId, UUID appealId, ReviewAccountLockAppealRequest request) {
        AccountLockAppeal appeal = appealRepository.findByIdAndStatus(appealId, AccountLockAppealStatus.PENDING)
                .orElseThrow(() -> new ValidationException("IAM-114-APPEAL-005: Appeal is not pending"));
        User user = userRepository.findByIdForUpdate(appeal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("IAM-114-003: User not found"));
        if (!user.isLocked() || user.getLockType() != AccountLockType.ADMIN
                || !appeal.getLockEpisodeId().equals(user.getLockEpisodeId())) {
            throw new ValidationException("IAM-114-APPEAL-002: Lock episode is no longer active");
        }

        Instant now = Instant.now();
        appeal.setReviewedBy(reviewerId);
        appeal.setReviewedAt(now);
        appeal.setReviewNote(request.reviewNote() == null ? null : request.reviewNote().trim());
        if (request.decision() == ReviewAccountLockAppealRequest.Decision.APPROVE) {
            appeal.setStatus(AccountLockAppealStatus.APPROVED);
            clearLock(user);
            userRepository.save(user);
        } else {
            appeal.setStatus(AccountLockAppealStatus.REJECTED);
        }
        AccountLockAppeal saved = appealRepository.save(appeal);
        auditService.log(AuditAction.ACCOUNT_LOCK_APPEAL_REVIEWED, reviewerId,
                "ACCOUNT_LOCK_APPEAL", saved.getId().toString(),
                Map.of("decision", request.decision().name(), "userId", user.getId()));
        return map(saved, user);
    }

    private void clearLock(User user) {
        user.setLocked(false);
        user.setLockedAt(null);
        user.setLockType(null);
        user.setLockReason(null);
        user.setLockedBy(null);
        user.setLockEpisodeId(null);
    }

    private AccountLockAppealResponse map(AccountLockAppeal appeal, User user) {
        return AccountLockAppealResponse.builder()
                .id(appeal.getId())
                .userId(appeal.getUserId())
                .userName(user == null ? null : user.getName())
                .userEmail(user == null ? null : user.getEmail())
                .lockEpisodeId(appeal.getLockEpisodeId())
                .lockReason(user == null ? null : user.getLockReason())
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .submittedAt(appeal.getSubmittedAt())
                .reviewedBy(appeal.getReviewedBy())
                .reviewedAt(appeal.getReviewedAt())
                .reviewNote(appeal.getReviewNote())
                .build();
    }
}

