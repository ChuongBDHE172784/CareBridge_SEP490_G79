package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidPostureConfigException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public InvalidPostureConfigException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static InvalidPostureConfigException doesNotSupportPostureAnalysis() {
        return new InvalidPostureConfigException(
                "PAC-005",
                "Exercise does not support posture analysis",
                HttpStatus.CONFLICT);
    }

    public static InvalidPostureConfigException alreadyExists() {
        return new InvalidPostureConfigException(
                "PAC-006",
                "A posture analysis config already exists for this exercise. "
                        + "Use POST /api/v1/admin/posture-configs/{exerciseId}/versions to create a new version.",
                HttpStatus.CONFLICT);
    }
}
