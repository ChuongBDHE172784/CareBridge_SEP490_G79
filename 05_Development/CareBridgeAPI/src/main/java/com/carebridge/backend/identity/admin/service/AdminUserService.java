package com.carebridge.backend.identity.admin.service;

import com.carebridge.backend.identity.admin.dto.request.AdminUserSearchQuery;
import com.carebridge.backend.identity.admin.dto.request.UpdateUserStatusRequest;
import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * UC114 Manage User Accounts — service contract (TDS CB-IDENTITY-IMP-114 §8.1).
 */
public interface AdminUserService {

    /**
     * Search/filter/paginate platform user accounts for the admin portal.
     */
    Page<AdminUserSummaryResponse> searchUsers(AdminUserSearchQuery query, Pageable pageable);

    /**
     * Enable/disable or lock/unlock a target user's account.
     *
     * @throws com.carebridge.backend.common.exception.AccessDeniedBusinessException IAM-114-004 if targetUserId == callerUserId
     * @throws com.carebridge.backend.common.exception.ResourceNotFoundException IAM-114-003 if targetUserId does not exist
     * @throws com.carebridge.backend.common.exception.ValidationException IAM-114-002 if both enabled/locked are omitted
     */
    AdminUserSummaryResponse updateStatus(UUID callerUserId, UUID targetUserId, UpdateUserStatusRequest request);
}
