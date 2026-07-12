package com.carebridge.backend.health.device.controller;

import com.carebridge.backend.common.response.ErrorResponse;
import com.carebridge.backend.health.device.exception.DeviceOperationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        DeviceConnectionController.class,
        DeviceMetricController.class,
        DeviceSyncController.class
})
public class DeviceExceptionHandler {

    @ExceptionHandler(DeviceOperationException.class)
    public ResponseEntity<ErrorResponse> handle(DeviceOperationException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
}
