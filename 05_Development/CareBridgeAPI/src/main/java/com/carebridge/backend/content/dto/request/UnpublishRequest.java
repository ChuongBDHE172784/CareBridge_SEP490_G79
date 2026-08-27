package com.carebridge.backend.content.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UnpublishRequest(@NotBlank(message = "CNT-011") String reason) {}
