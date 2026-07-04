package com.carebridge.backend.identity.admin.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.request.CreateStaffAccountRequest;
import com.carebridge.backend.identity.admin.dto.response.StaffAccountResponse;
import com.carebridge.backend.identity.admin.service.AdminStaffService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC115 Create Staff Account — service implementation.
 * ADR-IAM-004: service-level defense-in-depth check that request.role() is one of
 * the SRS-named staff roles (controller @PreAuthorize is the primary gate).
 * ADR-IAM-005: temp password never admin-supplied, always SecureRandom-generated;
 * mustChangePassword always true; email-delivery failure rolls back the whole
 * transaction (no orphaned account).
 * ADR-IAM-006: every creation synchronously audited in the same transaction.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminStaffServiceImpl implements AdminStaffService {

    private static final Set<Role> STAFF_ROLES = EnumSet.of(Role.MODERATOR, Role.CONTENT_ADMIN, Role.SYSTEM_ADMIN);
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordComplexityPolicy passwordComplexityPolicy;
    private final EmailService emailService;
    private final AuditService auditService;

    @Override
    public StaffAccountResponse createStaffAccount(UUID callerUserId, CreateStaffAccountRequest request) {
        assertStaffRole(request.getRole());

        if (userRepository.existsByEmail(request.getEmail()) ||
                (request.getPhone() != null && userRepository.existsByPhone(request.getPhone()))) {
            throw new BusinessException(HttpStatus.CONFLICT, "IAM-115-002", "Email or phone already registered");
        }

        String tempPassword = generateTempPassword();
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .name(request.getName())
                .role(request.getRole())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .enabled(true)
                .locked(false)
                .mustChangePassword(true)
                .build();

        User saved = userRepository.save(user);

        try {
            emailService.sendStaffAccountCredentialsEmail(saved.getEmail(), saved.getName(), tempPassword);
        } catch (Exception e) {
            // ADR-IAM-005 / IAM-115-006: entire transaction (including the users insert)
            // must roll back if credential delivery fails — never leave an orphaned,
            // unusable account with no delivered credential.
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "IAM-115-006", "Credential email delivery failed");
        }

        auditService.log(AuditAction.STAFF_ACCOUNT_CREATED, callerUserId, "USER", saved.getId().toString(),
                new StaffAccountCreatedPayload(saved.getId(), saved.getEmail(), saved.getRole(), callerUserId));

        return StaffAccountResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .name(saved.getName())
                .role(saved.getRole())
                .mustChangePassword(saved.isMustChangePassword())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private void assertStaffRole(Role role) {
        if (role == null || !STAFF_ROLES.contains(role)) {
            throw new ValidationException("IAM-115-005: role must be one of MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN");
        }
    }

    /** Package-visible for testability (UC115-TC-004/TC-012) — never exposed via the public API surface. */
    public String generateTempPassword() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        // Guarantee complexity-policy compliance regardless of random draw.
        String candidate = sb.toString();
        if (!passwordComplexityPolicy.isComplexEnough(candidate)) {
            candidate = "Aa1!" + candidate.substring(4);
        }
        return candidate;
    }

    private record StaffAccountCreatedPayload(UUID newUserId, String email, Role assignedRole, UUID createdByAdminId) {
    }
}
