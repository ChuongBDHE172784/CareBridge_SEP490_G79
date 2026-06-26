package com.carebridge.backend.security.dto.response;

import com.carebridge.backend.security.rbac.Role;
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
    private String phone;
    private String email;
    private String name;
    private String avatarUrl;
    private Role role;
}
