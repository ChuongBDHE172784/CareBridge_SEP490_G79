package com.carebridge.backend.safety.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SafetyException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public SafetyException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
