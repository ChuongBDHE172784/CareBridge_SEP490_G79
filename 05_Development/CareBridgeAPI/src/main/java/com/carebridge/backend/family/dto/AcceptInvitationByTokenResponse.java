package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AcceptInvitationByTokenResponse {
    private UUID careGroupId;
    private UUID careGroupMemberId;
    private String inviteStatus;
    private Instant joinedAt;
}
