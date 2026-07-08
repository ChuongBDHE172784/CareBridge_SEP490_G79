package com.carebridge.backend.family.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CareGroupSummaryDto {

    private UUID groupId;
    private String groupName;
    private boolean isActive;
    private int totalMembers;
    private String myRole;
}
