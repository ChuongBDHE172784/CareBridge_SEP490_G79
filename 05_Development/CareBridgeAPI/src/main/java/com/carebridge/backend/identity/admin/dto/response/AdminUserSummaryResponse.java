package com.carebridge.backend.identity.admin.dto.response;

import com.carebridge.backend.security.entity.AccountLockType;
import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UC114 Manage User Accounts — output DTO. NEVER exposes passwordHash (C2 / CWE-200).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {
    private UUID id;
    private String email;
    private String phone;
    private String name;
    private Role role;
    private boolean enabled;
    private boolean locked;
    private Instant lockedAt;
    private AccountLockType lockType;
    private String lockReason;
    private UUID lockedBy;
    private UUID lockEpisodeId;
    private Instant createdAt;
}
