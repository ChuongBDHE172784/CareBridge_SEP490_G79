package com.carebridge.backend.family.dto;

import com.carebridge.backend.family.entity.InviteChannel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteFamilyMemberRequest {

    @NotNull
    private InviteChannel channel;

    private String phone;
}
