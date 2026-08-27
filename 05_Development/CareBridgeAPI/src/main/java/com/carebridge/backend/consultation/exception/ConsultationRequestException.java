package com.carebridge.backend.consultation.exception;

import org.springframework.http.HttpStatus;

public class ConsultationRequestException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ConsultationRequestException(String code, String message, HttpStatus httpStatus) {
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

    public static ConsultationRequestException expertNotEligible() {
        return new ConsultationRequestException(
                "CONREQ-002", "Expert is not eligible for consultation", HttpStatus.CONFLICT);
    }

    public static ConsultationRequestException expertNoLongerEligible() {
        return new ConsultationRequestException(
                "CONREQ-004", "Expert is no longer eligible for consultation", HttpStatus.CONFLICT);
    }

    public static ConsultationRequestException invalidTransition() {
        return new ConsultationRequestException(
                "CONREQ-005", "Consultation request is no longer pending", HttpStatus.CONFLICT);
    }

    public static ConsultationRequestException expertNotFound() {
        return new ConsultationRequestException(
                "CONREQ-006", "Expert profile not found", HttpStatus.NOT_FOUND);
    }

    public static ConsultationRequestException notFound() {
        return new ConsultationRequestException(
                "CONREQ-007", "Consultation request not found", HttpStatus.NOT_FOUND);
    }

    public static ConsultationRequestException idempotencyConflict() {
        return new ConsultationRequestException(
                "CONREQ-009", "clientRequestId already used with different payload", HttpStatus.CONFLICT);
    }

    public static ConsultationRequestException availabilityNoLongerAvailable() {
        return new ConsultationRequestException(
                "CONREQ-010",
                "Selected expert availability is no longer available",
                HttpStatus.CONFLICT);
    }
}
