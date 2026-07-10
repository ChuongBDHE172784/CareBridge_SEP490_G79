package com.carebridge.backend.family.dto;

import com.carebridge.backend.family.entity.InviteChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteFamilyMemberRequest {

    @NotNull
    private InviteChannel channel;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid phone number format")
    private String phone;
}
