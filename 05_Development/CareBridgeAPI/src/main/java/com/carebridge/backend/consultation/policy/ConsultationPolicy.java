package com.carebridge.backend.consultation.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.stereotype.Component;

/**
 * Consultation Policy - Authorization rules for consultation operations.
 */
@Component
public class ConsultationPolicy {

    /**
     * Check if user can book a consultation with the given expert.
     *
     * @param expert the expert
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanBookConsultation(Long expertId, Long requestingUserId, Role requestingRole) {
        // Only regular users (MOTHER/FAMILY) can book
        if (requestingRole != Role.MOTHER && requestingRole != Role.FAMILY) {
            throw new AccessDeniedBusinessException("Only users can book consultations");
        }

        // User must be authenticated (requestingUserId must be valid)
        if (requestingUserId == null) {
            throw new AccessDeniedBusinessException("Authentication required");
        }
    }

    /**
     * Check if user can view a consultation.
     *
     * @param consultation the consultation
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanViewConsultation(Consultation consultation, Long requestingUserId, Role requestingRole) {
        if (consultation == null) {
            throw new ResourceNotFoundException("Consultation not found");
        }

        // ADMIN can view any consultation
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Expert can view consultations they are involved in
        if (requestingRole == Role.EXPERT && consultation.getExpertId().equals(requestingUserId)) {
            return;
        }

        // Requester can view their own consultations
        if (consultation.getRequesterUserId().equals(requestingUserId)) {
            return;
        }

        throw new AccessDeniedBusinessException("Insufficient permissions to view consultation");
    }

    /**
     * Check if user can update consultation (cancel/reschedule).
     *
     * @param consultation the consultation
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanUpdateConsultation(Consultation consultation, Long requestingUserId, Role requestingRole) {
        if (consultation == null) {
            throw new ResourceNotFoundException("Consultation not found");
        }

        // ADMIN can update any consultation
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Requester can cancel/reschedule their own booking
        if (consultation.getRequesterUserId().equals(requestingUserId)) {
            // Only allow if status is CONFIRMED or PENDING_PAYMENT
            if (consultation.getStatus() == com.carebridge.backend.expert.enums.ConsultationStatus.CONFIRMED ||
                    consultation.getStatus() == com.carebridge.backend.expert.enums.ConsultationStatus.PENDING_PAYMENT) {
                return;
            }
            throw new AccessDeniedBusinessException("Cannot modify consultation in current status");
        }

        // Expert can modify their own consultations
        if (requestingRole == Role.EXPERT && consultation.getExpertId().equals(requestingUserId)) {
            return;
        }

        throw new AccessDeniedBusinessException("Only the requester, expert, or admin can modify consultation");
    }

    /**
     * Check if user can join a consultation session.
     *
     * @param consultation the consultation
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanJoinSession(Consultation consultation, Long requestingUserId, Role requestingRole) {
        if (consultation == null) {
            throw new ResourceNotFoundException("Consultation not found");
        }

        // Must be CONFIRMED status
        if (consultation.getStatus() != com.carebridge.backend.expert.enums.ConsultationStatus.CONFIRMED) {
            throw new AccessDeniedBusinessException("Consultation is not confirmed");
        }

        // ADMIN can join any session (for support/audit)
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Expert can join their own consultations
        if (requestingRole == Role.EXPERT && consultation.getExpertId().equals(requestingUserId)) {
            return;
        }

        // Requester can join their own consultations
        if (consultation.getRequesterUserId().equals(requestingUserId)) {
            return;
        }

        throw new AccessDeniedBusinessException("Only the expert or requester can join the session");
    }

    /**
     * Check if user can view messages in a session.
     *
     * @param consultation the consultation
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanViewMessages(Consultation consultation, Long requestingUserId, Role requestingRole) {
        ensureCanViewConsultation(consultation, requestingUserId, requestingRole);
    }

    /**
     * Check if user can send message in a session.
     *
     * @param consultation the consultation
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanSendMessage(Consultation consultation, Long requestingUserId, Role requestingRole) {
        ensureCanJoinSession(consultation, requestingUserId, requestingRole);
    }
}
