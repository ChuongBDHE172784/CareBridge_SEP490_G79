package com.carebridge.backend.payment.policy;

import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.payment.entity.CommissionRecord;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.stereotype.Component;

/**
 * Payment Policy - Authorization rules for payment operations.
 */
@Component
public class PaymentPolicy {

    /**
     * Check if user can process payment for a consultation.
     *
     * @param consultation the consultation
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanProcessPayment(Consultation consultation, Long requestingUserId, Role requestingRole) {
        if (consultation == null) {
            throw new ResourceNotFoundException("Consultation not found");
        }

        // Only the requester can pay
        if (!consultation.getRequesterUserId().equals(requestingUserId)) {
            throw new AccessDeniedBusinessException("Only the consultation requester can make payment");
        }

        // Must be in PENDING_PAYMENT status
        if (consultation.getStatus() != com.carebridge.backend.expert.enums.ConsultationStatus.PENDING_PAYMENT) {
            throw new AccessDeniedBusinessException("Consultation is not in pending payment status");
        }
    }

    /**
     * Check if user can view payment transaction.
     *
     * @param paymentTransaction the payment transaction
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanViewPayment(com.carebridge.backend.payment.entity.PaymentTransaction paymentTransaction,
                                     Long requestingUserId, Role requestingRole) {
        if (paymentTransaction == null) {
            throw new ResourceNotFoundException("Payment transaction not found");
        }

        // ADMIN can view any payment
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Requester can view their own payment
        if (paymentTransaction.getPayerUserId().equals(requestingUserId)) {
            return;
        }

        throw new AccessDeniedBusinessException("Insufficient permissions to view payment");
    }

    /**
     * Check if user can issue a refund.
     *
     * @param paymentTransaction the payment transaction
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanIssueRefund(com.carebridge.backend.payment.entity.PaymentTransaction paymentTransaction,
                                     Long requestingUserId, Role requestingRole) {
        if (paymentTransaction == null) {
            throw new ResourceNotFoundException("Payment transaction not found");
        }

        // Only ADMIN can issue refunds
        if (requestingRole != Role.SYSTEM_ADMIN) {
            throw new AccessDeniedBusinessException("Only administrators can issue refunds");
        }

        // Cannot refund if no payment completed
        if (paymentTransaction.getStatus() != com.carebridge.backend.expert.enums.PaymentStatus.COMPLETED) {
            throw new AccessDeniedBusinessException("Cannot refund a non-completed payment");
        }
    }

    /**
     * Check if user can view commission records.
     *
     * @param commission the commission record
     * @param requestingUserId the user making the request
     * @param requestingRole the user's role
     */
    public void ensureCanViewCommission(CommissionRecord commission, Long requestingUserId, Role requestingRole) {
        if (commission == null) {
            throw new ResourceNotFoundException("Commission record not found");
        }

        // ADMIN can view any commission
        if (requestingRole == Role.SYSTEM_ADMIN) {
            return;
        }

        // Expert can view their own commissions
        if (requestingRole == Role.EXPERT && commission.getExpertId().equals(requestingUserId)) {
            return;
        }

        throw new AccessDeniedBusinessException("Insufficient permissions to view commission");
    }

    /**
     * Check if user can create settlement.
     *
     * @param requestingRole the user's role
     */
    public void ensureCanCreateSettlement(Role requestingRole) {
        // Only ADMIN can create settlements
        if (requestingRole != Role.SYSTEM_ADMIN) {
            throw new AccessDeniedBusinessException("Only administrators can create settlements");
        }
    }
}
