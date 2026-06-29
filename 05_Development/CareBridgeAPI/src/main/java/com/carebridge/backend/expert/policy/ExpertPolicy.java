package com.carebridge.backend.expert.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.expert.entity.Expert;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.stereotype.Component;

/**
 * Expert Policy - Authorization rules for expert-related operations.
 *
 * BR-01: RBAC enforcement for expert profile management.
 * - Only EXPERT (own) and ADMIN can access/modify expert profiles
 */
@Component
public class ExpertPolicy {

    /**
     * Check if user can access the given expert profile.
     *
     * @param expert the expert profile
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     * @throws AccessDeniedBusinessException if user is not authorized
     */
    public void ensureCanViewExpert(Expert expert, Long requestingUserId, Role requestingRole) {
        if (expert == null) {
            throw new ResourceNotFoundException("Expert not found");
        }

        // ADMIN can view any expert
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Expert can view own profile
        if (requestingRole == Role.EXPERT && expert.getUserId().equals(requestingUserId)) {
            return;
        }

        // USER can view APPROVED experts (public view)
        if (requestingRole == Role.MOTHER || requestingRole == Role.FAMILY) {
            if (expert.getVerificationStatus() == com.carebridge.backend.expert.enums.ExpertVerificationStatus.APPROVED) {
                return;
            }
            throw new AccessDeniedBusinessException("Expert profile not available for viewing");
        }

        throw new AccessDeniedBusinessException("Insufficient permissions to view expert profile");
    }

    /**
     * Check if user can update expert profile.
     *
     * @param expert the expert profile
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanUpdateExpert(Expert expert, Long requestingUserId, Role requestingRole) {
        if (expert == null) {
            throw new ResourceNotFoundException("Expert not found");
        }

        // ADMIN can update any expert
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Only the expert owner can update their own profile
        if (requestingRole == Role.EXPERT && expert.getUserId().equals(requestingUserId)) {
            return;
        }

        throw new AccessDeniedBusinessException("Only the expert or admin can update this profile");
    }

    /**
     * Check if user can upload verification documents.
     *
     * @param expert the expert profile
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanUploadDocuments(Expert expert, Long requestingUserId, Role requestingRole) {
        if (expert == null) {
            throw new ResourceNotFoundException("Expert not found");
        }

        // Expert can upload to own profile only
        if (requestingRole == Role.EXPERT && expert.getUserId().equals(requestingUserId)) {
            return;
        }

        // ADMIN can upload on behalf
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        throw new AccessDeniedBusinessException("Only the expert or admin can upload verification documents");
    }

    /**
     * Check if user can configure availability.
     *
     * @param expert the expert profile
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanConfigureAvailability(Expert expert, Long requestingUserId, Role requestingRole) {
        // Only verified experts can configure availability
        if (expert != null && expert.getVerificationStatus() !=
                com.carebridge.backend.expert.enums.ExpertVerificationStatus.APPROVED) {
            throw new AccessDeniedBusinessException("Expert must be approved to configure availability");
        }

        // EXPERT can configure own availability
        if (requestingRole == Role.EXPERT && expert != null && expert.getUserId().equals(requestingUserId)) {
            return;
        }

        // ADMIN can configure for any expert
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        throw new AccessDeniedBusinessException("Only approved experts or admin can configure availability");
    }
}
