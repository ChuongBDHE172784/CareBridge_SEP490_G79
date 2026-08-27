package com.carebridge.backend.common.exception;

public class RevokedSessionException extends RuntimeException {

    public RevokedSessionException(String message) {
        super(message);
    }
}
