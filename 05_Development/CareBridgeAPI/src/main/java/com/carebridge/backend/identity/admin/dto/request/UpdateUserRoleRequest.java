package com.carebridge.backend.identity.admin.dto.request;

import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UC116 Update Role and Permission — input DTO. Deliberately has NO `enabled` field
 * (UC114's exclusive field, TDS §1.1 write-surface non-overlap) — `lockAccessRights`
 * is a distinct, differently-named field.
 */
@Getter
@Setter
public class UpdateUserRoleRequest {

    @NotNull
    private Role newRole;

    private Boolean lockAccessRights;

    @Size(max = 500)
    private String reason;
}
