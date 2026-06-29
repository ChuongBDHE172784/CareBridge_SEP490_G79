package com.carebridge.backend.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RequestAccountDeletionRequest {

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @Size(max = 500)
    private String reason;
}
