package com.carebridge.backend.security.dto.request;

import com.carebridge.backend.common.validation.VietnamesePhoneNumber;
import com.carebridge.backend.security.rbac.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 2, max = 120)
    private String name;

    @VietnamesePhoneNumber
    private String phone;

    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    private Role role;
}
