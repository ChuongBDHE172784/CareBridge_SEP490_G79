package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SessionNotCompletedException extends RuntimeException {

    private final String code = "EXSESS-007";
    private final HttpStatus httpStatus = HttpStatus.CONFLICT;

    public SessionNotCompletedException() {
        super("Session is not yet completed");
    }
}
