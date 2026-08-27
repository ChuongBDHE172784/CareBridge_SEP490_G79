package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PostureConfigNotFoundException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public PostureConfigNotFoundException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    public static PostureConfigNotFoundException notFound() {
        return new PostureConfigNotFoundException(
                "PAC-004", "Posture analysis config version not found");
    }

    public static PostureConfigNotFoundException noActiveConfig() {
        return new PostureConfigNotFoundException(
                "PAC-004",
                "No active posture analysis config exists for this exercise — "
                        + "use POST /api/v1/admin/posture-configs first");
    }
}
