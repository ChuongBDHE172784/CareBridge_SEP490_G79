package com.carebridge.backend.content.exception;

import org.springframework.http.HttpStatus;

public class ModerationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ModerationException(String code, String message, HttpStatus httpStatus) {
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

    public static ModerationException pageSizeExceeded() {
        return new ModerationException(
                "MOD-002",
                "Page size exceeds maximum allowed value of 50",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException internalError() {
        return new ModerationException(
                "MOD-005",
                "Internal moderation service error",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
