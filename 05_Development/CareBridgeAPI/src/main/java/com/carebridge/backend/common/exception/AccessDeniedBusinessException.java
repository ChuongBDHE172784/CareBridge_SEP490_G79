package com.carebridge.backend.common.exception;

public class AccessDeniedBusinessException extends RuntimeException {

    public AccessDeniedBusinessException(String message) {
        super(message);
    }
}
