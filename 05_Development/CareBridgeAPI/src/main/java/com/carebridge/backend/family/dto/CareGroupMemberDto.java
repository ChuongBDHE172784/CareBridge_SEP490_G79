package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CareGroupMemberDto {

    private UUID memberId;
    private UUID userId;
    private String displayName;
    private String memberRole;
    private String inviteStatus;
    private Boolean isJoinRequest;
    private Instant joinedAt;
}
