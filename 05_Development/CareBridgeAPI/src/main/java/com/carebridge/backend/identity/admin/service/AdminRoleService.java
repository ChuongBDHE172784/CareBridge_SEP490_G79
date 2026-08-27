package com.carebridge.backend.identity.admin.service;

import com.carebridge.backend.identity.admin.dto.request.UpdateUserRoleRequest;
import com.carebridge.backend.identity.admin.dto.response.UserRoleResponse;
import java.util.UUID;

/**
 * UC116 Update Role and Permission — service contract (TDS CB-IDENTITY-IMP-116 §8.1).
 */
public interface AdminRoleService {

    /**
     * Reassign a target user's role and/or lock/unlock their access rights.
     *
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException IAM-116-004 if targetUserId == callerUserId (unconditional, ADR-IAM-007)
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException IAM-116-003 if targetUserId does not exist
     */
    UserRoleResponse updateRole(UUID callerUserId, UUID targetUserId, UpdateUserRoleRequest request);
}
