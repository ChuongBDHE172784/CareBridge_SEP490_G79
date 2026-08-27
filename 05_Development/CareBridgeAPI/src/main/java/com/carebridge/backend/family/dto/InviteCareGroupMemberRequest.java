package com.carebridge.backend.family.dto;

import com.carebridge.backend.family.entity.GroupMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteCareGroupMemberRequest {

    @NotBlank
    @Email
    private String email;

    /** Defaults to MEMBER when omitted. */
    private GroupMemberRole memberRole;
}
