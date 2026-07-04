package com.carebridge.backend.identity.admin.service;

import com.carebridge.backend.identity.admin.dto.request.CreateStaffAccountRequest;
import com.carebridge.backend.identity.admin.dto.response.StaffAccountResponse;
import java.util.UUID;

/**
 * UC115 Create Staff Account — service contract (TDS CB-IDENTITY-IMP-115 §8.1).
 */
public interface AdminStaffService {

    /**
     * Create a new staff (MODERATOR/CONTENT_ADMIN/SYSTEM_ADMIN) account. Issues a
     * system-generated temporary password via email; never returns the password in
     * the API response (ADR-IAM-005).
     *
     * @throws com.carebridge.backend.common.exception.ValidationException IAM-115-005 if request.role() is not a staff role
     * @throws com.carebridge.backend.common.exception.BusinessException IAM-115-002 if email/phone already exists
     */
    StaffAccountResponse createStaffAccount(UUID callerUserId, CreateStaffAccountRequest request);
}
