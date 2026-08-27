package com.carebridge.backend.map.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MapException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String code;

    public MapException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
