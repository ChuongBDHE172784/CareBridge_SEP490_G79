package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidSessionStateException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus = HttpStatus.CONFLICT;

    public InvalidSessionStateException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static InvalidSessionStateException alreadyPaused() {
        return new InvalidSessionStateException(
                "EXSESS-005", "Session is not IN_PROGRESS, cannot pause");
    }

    public static InvalidSessionStateException notPaused() {
        return new InvalidSessionStateException(
                "EXSESS-006", "Session is not PAUSED, cannot resume");
    }

    public static InvalidSessionStateException cannotComplete() {
        return new InvalidSessionStateException(
                "EXSESS-008", "Session is not active, cannot complete");
    }

    public static InvalidSessionStateException notInProgress() {
        return new InvalidSessionStateException(
                "EXSESS-009", "Session is not IN_PROGRESS, cannot submit posture event");
    }
}
