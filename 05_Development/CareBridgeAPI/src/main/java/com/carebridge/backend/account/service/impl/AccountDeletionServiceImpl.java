package com.carebridge.backend.account.service.impl;

import com.carebridge.backend.account.dto.request.RequestAccountDeletionRequest;
import com.carebridge.backend.account.dto.response.AccountDeletionRequestResponse;
import com.carebridge.backend.account.entity.AccountDeletionRequest;
import com.carebridge.backend.account.entity.DeletionStatus;
import com.carebridge.backend.account.repository.AccountDeletionRequestRepository;
import com.carebridge.backend.account.service.AccountDeletionService;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.AuthorizationException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountDeletionServiceImpl implements AccountDeletionService {

    private static final int DELETION_GRACE_PERIOD_DAYS = 30;

    private final AccountDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public AccountDeletionRequestResponse requestDeletion(UUID userId, RequestAccountDeletionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Block SYSTEM_ADMIN
        if (user.getRole() == Role.SYSTEM_ADMIN) {
            throw new AuthorizationException("SYSTEM_ADMIN accounts cannot be deleted");
        }

        // Already has a pending request
        if (deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PENDING)) {
            throw new ValidationException("DEL-001: A pending deletion request already exists");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getConfirmPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("DEL-002: Incorrect password");
        }

        Instant now = Instant.now();
        AccountDeletionRequest deletionRequest = AccountDeletionRequest.builder()
                .userId(userId)
                .status(DeletionStatus.PENDING)
                .reason(request.getReason())
                .requestedAt(now)
                .scheduledFor(now.plus(DELETION_GRACE_PERIOD_DAYS, ChronoUnit.DAYS))
                .build();

        AccountDeletionRequest saved = deletionRequestRepository.save(deletionRequest);

        auditService.log(AuditAction.SECURITY_EVENT, userId, "AccountDeletionRequest",
                saved.getId().toString(),
                Map.of("action", "DELETION_REQUESTED", "scheduledFor", saved.getScheduledFor().toString()));

        return toResponse(saved);
    }

    @Override
    public AccountDeletionRequestResponse cancelDeletion(UUID userId) {
        AccountDeletionRequest request = deletionRequestRepository
                .findByUserIdAndStatus(userId, DeletionStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("No pending deletion request found"));

        request.setStatus(DeletionStatus.CANCELLED);
        request.setProcessedAt(Instant.now());
        AccountDeletionRequest saved = deletionRequestRepository.save(request);

        auditService.log(AuditAction.SECURITY_EVENT, userId, "AccountDeletionRequest",
                saved.getId().toString(),
                Map.of("action", "DELETION_CANCELLED"));

        return toResponse(saved);
    }

    private AccountDeletionRequestResponse toResponse(AccountDeletionRequest request) {
        return AccountDeletionRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .status(request.getStatus())
                .reason(request.getReason())
                .requestedAt(request.getRequestedAt())
                .scheduledFor(request.getScheduledFor())
                .processedAt(request.getProcessedAt())
                .build();
    }
}
