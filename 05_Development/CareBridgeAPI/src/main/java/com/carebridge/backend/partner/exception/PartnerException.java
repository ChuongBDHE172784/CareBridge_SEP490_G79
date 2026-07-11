package com.carebridge.backend.partner.exception;

import org.springframework.http.HttpStatus;

public class PartnerException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public PartnerException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static PartnerException profileAlreadyExists() {
        return new PartnerException(
                "PTR-002",
                "A partner profile already exists for this account",
                HttpStatus.CONFLICT);
    }

    public static PartnerException emailAlreadyRegistered() {
        return new PartnerException(
                "PTR-003",
                "This email address is already registered to another organization",
                HttpStatus.CONFLICT);
    }

    public static PartnerException profileNotFound() {
        return new PartnerException("PTR-007", "No partner profile found for the current user", HttpStatus.NOT_FOUND);
    }

    public static PartnerException profileNotEditable() {
        return new PartnerException("PTR-009", "Profile cannot be edited in its current status", HttpStatus.CONFLICT);
    }

    public static PartnerException organizationNotFound() {
        return new PartnerException("PTR-010", "No partner organization found for the current user", HttpStatus.NOT_FOUND);
    }

    public static PartnerException organizationNotApproved() {
        return new PartnerException("PTR-011", "Partner organization must be APPROVED to submit", HttpStatus.CONFLICT);
    }
    public static PartnerException campaignOrganizationNotFound() { return new PartnerException("PTR-013", "No partner organization found for current user", HttpStatus.NOT_FOUND); }
    public static PartnerException campaignOrganizationNotApproved() { return new PartnerException("PTR-014", "Partner organization must be APPROVED to submit campaigns", HttpStatus.CONFLICT); }
    public static PartnerException invalidCampaignDates() { return new PartnerException("PTR-015", "Sponsored campaign validation failed", HttpStatus.BAD_REQUEST); }
    public static PartnerException performanceOrganizationNotFound(){return new PartnerException("PTR-016","No partner organization found for current user",HttpStatus.NOT_FOUND);}
    public static PartnerException invalidPerformanceRange(){return new PartnerException("PTR-017","Invalid date range",HttpStatus.BAD_REQUEST);}
    public static PartnerException approvalPartnerNotFound(){return new PartnerException("PTR-018","Partner organization not found",HttpStatus.NOT_FOUND);}
    public static PartnerException invalidStatusTransition(){return new PartnerException("PTR-020","Invalid status transition",HttpStatus.CONFLICT);}
    public static PartnerException decisionReasonRequired(){return new PartnerException("PTR-021","Reason required for REJECT/SUSPEND",HttpStatus.BAD_REQUEST);}
    public static PartnerException contentTargetNotFound(){return new PartnerException("PTR-022","Partner content target not found",HttpStatus.NOT_FOUND);}
    public static PartnerException unsupportedTargetType(){return new PartnerException("PTR-023","Unsupported target type",HttpStatus.BAD_REQUEST);}
    public static PartnerException contentAlreadyDecided(){return new PartnerException("PTR-024","Target is not in PENDING status",HttpStatus.CONFLICT);}
    public static PartnerException contentDecisionReasonRequired(){return new PartnerException("PTR-025","Reason required for REJECT",HttpStatus.BAD_REQUEST);}
    public static PartnerException removalTargetNotFound(){return new PartnerException("PTR-026","Partner content target not found",HttpStatus.NOT_FOUND);}
    public static PartnerException removalUnsupportedTargetType(){return new PartnerException("PTR-027","Unsupported target type",HttpStatus.BAD_REQUEST);}
    public static PartnerException contentAlreadyRemoved(){return new PartnerException("PTR-028","Content is already removed",HttpStatus.CONFLICT);}
    public static PartnerException removalReasonRequired(){return new PartnerException("PTR-029","Removal reason is required",HttpStatus.BAD_REQUEST);}
}
