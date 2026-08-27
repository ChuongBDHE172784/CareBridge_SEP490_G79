package com.carebridge.backend.identity.admin.dto.response;

import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {
    private UUID id;
    private Role previousRole;
    private Role newRole;
    private boolean locked;
    private Instant updatedAt;
}
