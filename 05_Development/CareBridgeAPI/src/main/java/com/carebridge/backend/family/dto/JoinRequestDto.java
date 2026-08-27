package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** DTO representing a self-initiated join request (by invite code) waiting for Mother's approval. */
@Data
@Builder
public class JoinRequestDto {

    private UUID memberId;
    private UUID userId;
    private String displayName;
    private String email;
    private String phone;
    private Instant requestedAt;
}
