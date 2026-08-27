package com.carebridge.backend.profile.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
    UUID userId,
    String displayName,
    String avatarUrl,
    String phoneNumber,
    LocalDate dateOfBirth,
    String area,
    Instant updatedAt
) {}
