package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SafetyCheckNotClearedException extends RuntimeException {

    private final String code = "EXSESS-003";
    private final HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;

    public SafetyCheckNotClearedException() {
        super("Safety check is not cleared for this exercise");
    }
}
