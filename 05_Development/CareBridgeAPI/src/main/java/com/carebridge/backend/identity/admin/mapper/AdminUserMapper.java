package com.carebridge.backend.identity.admin.mapper;

import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.security.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AdminUserMapper {

    public AdminUserSummaryResponse toSummary(User user) {
        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .lockedAt(user.getLockedAt())
                .lockType(user.getLockType())
                .lockReason(user.getLockReason())
                .lockedBy(user.getLockedBy())
                .lockEpisodeId(user.getLockEpisodeId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
