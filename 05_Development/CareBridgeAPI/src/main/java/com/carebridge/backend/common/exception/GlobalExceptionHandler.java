package com.carebridge.backend.common.exception;

import com.carebridge.backend.common.response.ErrorDetail;
import com.carebridge.backend.common.response.ErrorResponse;
import com.carebridge.backend.community.exception.CommunityFeedValidationException;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.DuplicateTopicNameException;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.partner.exception.PartnerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request body: " + ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
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
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(CommunityTopicNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommunityTopicNotFound(
            CommunityTopicNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "COM-003", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateTopicNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTopicName(
            DuplicateTopicNameException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "COM-009", ex.getMessage(), request);
    }

    @ExceptionHandler(QuestionNotAnswerableException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotAnswerable(
            QuestionNotAnswerableException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "COM-007", ex.getMessage(), request);
    }

    @ExceptionHandler(CommunityFeedValidationException.class)
    public ResponseEntity<ErrorResponse> handleCommunityFeedValidation(
            CommunityFeedValidationException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "COM-001", ex.getMessage(), request);
    }

    @ExceptionHandler(ContentException.class)
    public ResponseEntity<ErrorResponse> handleContent(ContentException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(PartnerException.class)
    public ResponseEntity<ErrorResponse> handlePartner(PartnerException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ConsentException.class)
    public ResponseEntity<ErrorResponse> handleConsent(ConsentException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "CONSENT_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "AUTHORIZATION_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Insufficient permissions", request);
    }

    @ExceptionHandler(AccessDeniedBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessAccess(
            AccessDeniedBusinessException ex,
            HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(RedFlagException.class)
    public ResponseEntity<ErrorResponse> handleRedFlag(RedFlagException ex, HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                "RED_FLAG_DETECTED",
                ex.getMessage() + " If this is an emergency, call 115 immediately.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
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
