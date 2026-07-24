package com.carebridge.backend.consultation.context.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class TriageExpertHandoffException extends BusinessException {

    private TriageExpertHandoffException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    public static TriageExpertHandoffException invalidRequest() {
        return new TriageExpertHandoffException(
                HttpStatus.BAD_REQUEST, "HANDOFF-001", "Invalid handoff request");
    }

    public static TriageExpertHandoffException sourceNotFound() {
        return new TriageExpertHandoffException(
                HttpStatus.NOT_FOUND, "HANDOFF-002", "Handoff source not found");
    }

    public static TriageExpertHandoffException intakeNotEligible() {
        return new TriageExpertHandoffException(
                HttpStatus.CONFLICT,
                "HANDOFF-003",
                "Intake is not eligible for expert handoff");
    }

    public static TriageExpertHandoffException expertNoLongerAvailable() {
        return new TriageExpertHandoffException(
                HttpStatus.CONFLICT, "HANDOFF-004", "Expert is no longer available");
    }

    public static TriageExpertHandoffException consentPolicyChanged() {
        return new TriageExpertHandoffException(
                HttpStatus.CONFLICT, "HANDOFF-005", "Consent policy changed");
    }

    public static TriageExpertHandoffException sharedContextNotFound() {
        return new TriageExpertHandoffException(
                HttpStatus.NOT_FOUND, "HANDOFF-006", "Shared context not found");
    }

    public static TriageExpertHandoffException sharedContextUnavailable() {
        return new TriageExpertHandoffException(
                HttpStatus.FORBIDDEN,
                "HANDOFF-007",
                "Shared context is no longer available");
    }

    public static TriageExpertHandoffException noApprovedContext() {
        return new TriageExpertHandoffException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "HANDOFF-008",
                "No approved context is available");
    }

    public static TriageExpertHandoffException idempotencyConflict() {
        return new TriageExpertHandoffException(
                HttpStatus.CONFLICT,
                "HANDOFF-009",
                "Idempotency key conflicts with another intent");
    }

    public static TriageExpertHandoffException completionFailed() {
        return new TriageExpertHandoffException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "HANDOFF-010",
                "Handoff could not be completed");
    }
}
