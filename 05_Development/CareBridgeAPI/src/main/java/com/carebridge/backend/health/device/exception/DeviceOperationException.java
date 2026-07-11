package com.carebridge.backend.health.device.exception;

import org.springframework.http.HttpStatus;

public class DeviceOperationException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public DeviceOperationException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
