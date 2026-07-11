package com.carebridge.backend.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokeInvitationResponse {
    private UUID careGroupMemberId;
    private UUID groupId;
    private UUID targetUserId;
    private String inviteStatus;
    private Instant revokedAt;
}
