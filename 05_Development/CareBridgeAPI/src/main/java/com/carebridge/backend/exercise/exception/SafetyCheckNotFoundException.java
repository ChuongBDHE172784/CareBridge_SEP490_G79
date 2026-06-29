package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SafetyCheckNotFoundException extends RuntimeException {

    private final String code = "PSC-002";
    private final HttpStatus httpStatus = HttpStatus.NOT_FOUND;

    public SafetyCheckNotFoundException() {
        super("No safety check found for this exercise");
    }
}
