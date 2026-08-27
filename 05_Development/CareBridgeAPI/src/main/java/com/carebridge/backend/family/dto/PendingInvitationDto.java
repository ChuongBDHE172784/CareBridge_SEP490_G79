package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PendingInvitationDto {

    private UUID groupId;
    private String groupName;
    private String memberRole;
    private Instant invitedAt;
}
