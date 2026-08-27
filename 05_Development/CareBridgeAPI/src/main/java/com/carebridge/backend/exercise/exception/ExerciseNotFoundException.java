package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExerciseNotFoundException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ExerciseNotFoundException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    public static ExerciseNotFoundException notFound() {
        return new ExerciseNotFoundException("EX-001", "Exercise not found");
    }
}
