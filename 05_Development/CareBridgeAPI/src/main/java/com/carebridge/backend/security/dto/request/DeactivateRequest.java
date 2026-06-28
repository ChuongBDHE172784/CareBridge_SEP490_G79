package com.carebridge.backend.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeactivateRequest {

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
