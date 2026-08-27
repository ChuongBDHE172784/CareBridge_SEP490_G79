package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.SharedDataResponse;
import com.carebridge.backend.family.entity.SharedDataCategory;

import java.util.UUID;

public interface ISharedDataService {

    /**
     * UC-84: Returns shared data for a care group member, filtered by category and permission.
     * Caller must be an ACCEPTED member with the category-specific permission flag set (ADR-FAM-003).
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) caller not ACCEPTED member
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-011/403) category permission denied
     */
    SharedDataResponse getSharedData(UUID groupId, UUID callerId, SharedDataCategory category,
                                     int page, int size);
}
