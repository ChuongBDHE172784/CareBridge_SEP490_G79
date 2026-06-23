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
}
