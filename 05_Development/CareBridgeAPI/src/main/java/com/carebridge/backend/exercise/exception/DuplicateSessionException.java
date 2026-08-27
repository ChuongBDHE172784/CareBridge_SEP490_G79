package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DuplicateSessionException extends RuntimeException {

    private final String code = "EXSESS-004";
    private final HttpStatus httpStatus = HttpStatus.CONFLICT;

    public DuplicateSessionException() {
        super("An active session already exists for this exercise today");
    }
}
