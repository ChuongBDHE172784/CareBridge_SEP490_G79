package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidExerciseStateException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public InvalidExerciseStateException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static InvalidExerciseStateException safetyWarningCannotBeBlanked() {
        return new InvalidExerciseStateException(
                "EX-ADMIN-002",
                "safetyWarning cannot be blanked",
                HttpStatus.BAD_REQUEST);
    }
}
