package com.carebridge.backend.consultation.policy;

import com.carebridge.backend.consultation.entity.ConsultationRequest;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.security.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConsultationRequestPolicy {

    public void assertExpertEligibleForConsultation(ExpertProfile expertProfile) {
        if (!expertProfile.isEligibleForConsultation()) {
            throw ConsultationRequestException.expertNotEligible();
        }
    }

    public void assertExpertEligibleForConsultation(
            ExpertProfile expertProfile, User expertAccount, Instant now) {
        assertExpertEligibleForConsultation(expertProfile);
        if (expertAccount == null
                || !expertAccount.isEnabled()
                || expertAccount.isLocked()
                || (expertAccount.getSuspendedUntil() != null
                        && expertAccount.getSuspendedUntil().isAfter(now))) {
            throw ConsultationRequestException.expertNotEligible();
        }
    }

    public void assertExpertStillEligibleForConsultation(
            ExpertProfile expertProfile, User expertAccount, Instant now) {
        if (!expertProfile.isEligibleForConsultation()
                || expertAccount == null
                || !expertAccount.isEnabled()
                || expertAccount.isLocked()
                || (expertAccount.getSuspendedUntil() != null
                        && expertAccount.getSuspendedUntil().isAfter(now))) {
            throw ConsultationRequestException.expertNoLongerEligible();
        }
    }

    public void assertCanView(
            ConsultationRequest request, UUID currentUserId, UUID assignedExpertUserId) {
        if (!currentUserId.equals(request.getRequesterUserId())
                && !currentUserId.equals(assignedExpertUserId)) {
            throw ConsultationRequestException.notFound();
        }
    }

    public void assertCanRespond(ConsultationRequest request, UUID expertUserId, UUID assignedExpertUserId) {
        if (!expertUserId.equals(assignedExpertUserId)) {
            throw ConsultationRequestException.notFound();
        }
    }

    public void assertCanCancel(ConsultationRequest request, UUID requesterUserId) {
        if (!requesterUserId.equals(request.getRequesterUserId())) {
            throw ConsultationRequestException.notFound();
        }
    }
}
