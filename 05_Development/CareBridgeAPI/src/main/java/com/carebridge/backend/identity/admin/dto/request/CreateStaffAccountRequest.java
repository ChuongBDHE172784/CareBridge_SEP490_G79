package com.carebridge.backend.identity.admin.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * UC115 Create Staff Account — input DTO. Deliberately has NO password/tempPassword
 * field (ADR-IAM-005 / C3): the admin never supplies or knows the new credential.
 */
@Getter
@Setter
public class CreateStaffAccountRequest {

    @Email
    @NotBlank
    private String email;

    @VietnamesePhoneNumber
    private String phone;

    @NotBlank
    @Size(max = 120)
    private String name;

    /** MUST be MODERATOR | CONTENT_ADMIN | SYSTEM_ADMIN — re-validated in the service (ADR-IAM-004). */
    @NotNull
    private Role role;
}
