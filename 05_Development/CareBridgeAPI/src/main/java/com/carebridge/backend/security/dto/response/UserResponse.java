package com.carebridge.backend.security.dto.response;

import com.carebridge.backend.security.rbac.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;

    private String email; // masked if needed

    private String phone; // masked if needed

    private Role role;

    private Instant createdAt;
}
