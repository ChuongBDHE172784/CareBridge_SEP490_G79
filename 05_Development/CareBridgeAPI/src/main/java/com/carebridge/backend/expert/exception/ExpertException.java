package com.carebridge.backend.expert.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExpertException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;

    public ExpertException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
