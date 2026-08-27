package com.carebridge.backend.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContinuationTokenRequest {
    @NotBlank
    @Size(max = 36)
    private String token;
}
