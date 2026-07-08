package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideContentRequest(
        @NotBlank @Size(max = 1000) String reason
) {}
