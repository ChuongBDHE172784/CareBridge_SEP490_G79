package com.carebridge.backend.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeactivateRequest {

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    /**
     * Optional. Persisted to users.deactivation_reason when supplied, which is
     * where the retired deletion-request reason now lives.
     */
    @Size(max = 1000, message = "Reason must be at most 1000 characters")
    private String reason;
}
