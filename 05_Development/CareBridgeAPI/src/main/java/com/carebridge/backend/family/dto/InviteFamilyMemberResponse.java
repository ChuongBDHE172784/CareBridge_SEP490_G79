package com.carebridge.backend.family.dto;

import com.carebridge.backend.family.entity.InviteChannel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InviteFamilyMemberResponse {

    private UUID careGroupMemberId;
    private InviteChannel channel;
    private String inviteToken;
    private Instant inviteExpiresAt;
    private String invitedPhone;
}
