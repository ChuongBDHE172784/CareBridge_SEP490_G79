package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectRoleRequest {

    @NotNull
    private Role role;
}
