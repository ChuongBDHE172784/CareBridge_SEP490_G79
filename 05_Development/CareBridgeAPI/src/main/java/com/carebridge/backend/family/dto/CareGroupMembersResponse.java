package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CareGroupMembersResponse {

    private UUID groupId;
    private String groupName;
    private int totalMembers;
    private List<CareGroupMemberDto> members;
}
