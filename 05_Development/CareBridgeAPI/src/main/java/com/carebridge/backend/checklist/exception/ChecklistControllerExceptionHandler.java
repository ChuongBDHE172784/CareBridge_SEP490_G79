package com.carebridge.backend.checklist.exception;

import com.carebridge.backend.checklist.controller.UserChecklistItemController;
import com.carebridge.backend.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserChecklistItemController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChecklistControllerExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String defaultMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && message.matches("CHECKLIST-\\d{3}:.*"))
                .findFirst()
                .orElse("CHECKLIST-001: Invalid checklist request");
        int separator = defaultMessage.indexOf(':');
        String code = separator > 0 ? defaultMessage.substring(0, separator) : "CHECKLIST-001";
        String message = separator > 0 ? defaultMessage.substring(separator + 1).trim()
                : "Invalid checklist request";
        return badRequest(code, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return badRequest("CHECKLIST-001", "Invalid checklist request", request);
    }

    private ResponseEntity<ErrorResponse> badRequest(
            String code, String message, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), code, message, request.getRequestURI()));
    }
}
