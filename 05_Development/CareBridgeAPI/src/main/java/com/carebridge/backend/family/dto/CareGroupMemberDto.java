package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CareGroupMemberDto {

    private UUID memberId;
    private String displayName;
    private String memberRole;
    private String inviteStatus;
    private Instant joinedAt;
}
