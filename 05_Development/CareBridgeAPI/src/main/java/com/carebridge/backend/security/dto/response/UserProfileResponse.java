package com.carebridge.backend.security.dto.response;

import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private java.util.UUID id;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private Role role;
    private String accountStatus;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Instant createdAt;
}
