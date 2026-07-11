package com.carebridge.backend.partner.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RemovalRequest(@NotBlank(message = "PTR-029") String reason) {}
