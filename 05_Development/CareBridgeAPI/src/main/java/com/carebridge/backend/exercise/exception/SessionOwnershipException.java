package com.carebridge.backend.exercise.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SessionOwnershipException extends RuntimeException {

    private final String code = "EXSESS-010";
    private final HttpStatus httpStatus = HttpStatus.FORBIDDEN;

    public SessionOwnershipException() {
        super("You are not the owner of this session");
    }
}
