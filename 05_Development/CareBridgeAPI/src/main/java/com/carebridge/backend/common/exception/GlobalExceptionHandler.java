package com.carebridge.backend.common.exception;

import com.carebridge.backend.common.response.ErrorDetail;
import com.carebridge.backend.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        logger.error("Validation error: {}", ex.getMessage(), ex);
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorDetail.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Invalid request")
                .path(request.getRequestURI())
                .details(details)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        logger.error("Constraint violation: {}", ex.getMessage(), ex);
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(violation -> ErrorDetail.builder()
                        .field(violation.getPropertyPath().toString())
                        .rejectedValue(violation.getInvalidValue())
                        .message(violation.getMessage())
                        .build())
                .toList();
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Invalid request")
                .path(request.getRequestURI())
                .details(details)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        logger.error("Validation exception: {}", ex.getMessage(), ex);
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {
        logger.error("Rate limit exceeded: {}", ex.getMessage(), ex);
        return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.error("Resource not found: {}", ex.getMessage(), ex);
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(ConsentException.class)
    public ResponseEntity<ErrorResponse> handleConsent(ConsentException ex, HttpServletRequest request) {
        logger.error("Consent denied: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "CONSENT_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        logger.error("Authentication failed: {}", ex.getMessage(), ex);
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(RevokedSessionException.class)
    public ResponseEntity<ErrorResponse> handleRevokedSession(RevokedSessionException ex, HttpServletRequest request) {
        logger.error("Session revoked: {}", ex.getMessage(), ex);
        return error(HttpStatus.UNAUTHORIZED, "SESSION_REVOKED", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        logger.error("Invalid refresh token: {}", ex.getMessage(), ex);
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage(), request);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFoundException ex, HttpServletRequest request) {
        logger.error("Session not found: {}", ex.getMessage(), ex);
        return error(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException ex, HttpServletRequest request) {
        logger.error("Account disabled: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        logger.error("Account locked: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationException ex, HttpServletRequest request) {
        logger.error("Authorization denied: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "AUTHORIZATION_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessAccess(
            AccessDeniedBusinessException ex,
            HttpServletRequest request) {
        logger.error("Business access denied: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(RedFlagException.class)
    public ResponseEntity<ErrorResponse> handleRedFlag(RedFlagException ex, HttpServletRequest request) {
        logger.error("Red flag detected: {}", ex.getMessage(), ex);
        return error(
                HttpStatus.BAD_REQUEST,
                "RED_FLAG_DETECTED",
                ex.getMessage() + " If this is an emergency, call 115 immediately.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred: {}", request.getRequestURI(), ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(status.value(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }
}
