package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;

import java.util.UUID;

public interface ICareGroupService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-002/409) if >= 5 active groups */
    CreateCareGroupResponse createCareGroup(CreateCareGroupRequest request, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) if not ACCEPTED member */
    CareGroupMembersResponse listMembers(UUID groupId, UUID callerId);
}
