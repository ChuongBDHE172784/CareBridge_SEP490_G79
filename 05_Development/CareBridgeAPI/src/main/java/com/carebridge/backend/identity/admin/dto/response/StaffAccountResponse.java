package com.carebridge.backend.identity.admin.dto.response;

import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UC115 Create Staff Account — output DTO. NEVER includes password/tempPassword (ADR-IAM-005).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAccountResponse {
    private UUID id;
    private String email;
    private String name;
    private Role role;
    private boolean mustChangePassword;
    private Instant createdAt;
}
