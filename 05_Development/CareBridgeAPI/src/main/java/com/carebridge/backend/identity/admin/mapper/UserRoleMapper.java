package com.carebridge.backend.identity.admin.mapper;

import com.carebridge.backend.identity.admin.dto.response.UserRoleResponse;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.entity.User;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class UserRoleMapper {

    public UserRoleResponse toResponse(User user, Role previousRole) {
        return UserRoleResponse.builder()
                .id(user.getId())
                .previousRole(previousRole)
                .newRole(user.getRole())
                .locked(user.isLocked())
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt() : Instant.now())
                .build();
    }
}
