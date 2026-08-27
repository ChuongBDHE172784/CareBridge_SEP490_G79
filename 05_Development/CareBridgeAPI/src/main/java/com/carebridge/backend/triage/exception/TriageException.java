package com.carebridge.backend.triage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TriageException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public TriageException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
