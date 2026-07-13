package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentSourceRequest(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 2000) String url,
        @Size(max = 255) String publisher) { }
