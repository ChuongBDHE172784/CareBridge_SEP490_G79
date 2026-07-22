package com.carebridge.backend.common.exception;

import com.carebridge.backend.common.response.ErrorDetail;
import com.carebridge.backend.common.response.ErrorResponse;
import com.carebridge.backend.community.exception.CommunityFeedValidationException;
import com.carebridge.backend.exercise.exception.DuplicateSessionException;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.exception.InvalidPostureConfigException;
import com.carebridge.backend.exercise.exception.InvalidSessionStateException;
import com.carebridge.backend.exercise.exception.PostureConfigNotFoundException;
import com.carebridge.backend.exercise.exception.SafetyCheckNotClearedException;
import com.carebridge.backend.exercise.exception.SafetyCheckNotFoundException;
import com.carebridge.backend.exercise.exception.SessionNotCompletedException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.DuplicateTopicNameException;
import com.carebridge.backend.community.exception.InvalidTopicHierarchyException;
import com.carebridge.backend.community.exception.QuestionNotAnswerableException;
import com.carebridge.backend.community.exception.QuestionNotFoundException;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.exception.ModerationException;
import com.carebridge.backend.integration.gemini.exception.RagException;
import com.carebridge.backend.partner.exception.PartnerException;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.safety.exception.SafetyException;
import com.carebridge.backend.triage.exception.RedFlagRuleException;
import com.carebridge.backend.triage.exception.TriageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request body", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        return error(HttpStatus.BAD_REQUEST, "MOD-001", message, request);
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

    // Spring 7 routes @RequestParam/@PathVariable constraint violations (e.g. @Min/@Max/@Size on
    // ExpertProfileController#getDirectory) through HandlerMethodValidationException, NOT the
    // classic jakarta.validation.ConstraintViolationException handled above — confirmed empirically
    // (CB-EXPCHAT-IMP-001 Logic Issue L12): without this handler, such violations fell through to
    // handleGeneric() and returned 500 INTERNAL_ERROR instead of 400. Same VALIDATION_ERROR shape
    // as handleConstraintViolation, for every existing @RequestParam-validated endpoint, not just this feature.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {
        logger.error("Request parameter validation error: {}", ex.getMessage(), ex);
        List<ErrorDetail> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> ErrorDetail.builder()
                                .field(result.getMethodParameter().getParameterName())
                                .rejectedValue(result.getArgument())
                                .message(error.getDefaultMessage())
                                .build()))
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

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        logger.error("Business exception [{}]: {}", ex.getCode(), ex.getMessage(), ex);
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        logger.error("Validation exception: {}", ex.getMessage(), ex);
        return error(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request);
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

    @ExceptionHandler(InvalidTopicHierarchyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTopicHierarchy(
            InvalidTopicHierarchyException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "COM-015", ex.getMessage(), request);
    }

    @ExceptionHandler(QuestionNotAnswerableException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotAnswerable(
            QuestionNotAnswerableException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "COM-007", ex.getMessage(), request);
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotFound(
            QuestionNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "COM-006", ex.getMessage(), request);
    }

    @ExceptionHandler(com.carebridge.backend.community.exception.AnswerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnswerNotFound(
            com.carebridge.backend.community.exception.AnswerNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "COM-011", ex.getMessage(), request);
    }

    @ExceptionHandler(CommunityFeedValidationException.class)
    public ResponseEntity<ErrorResponse> handleCommunityFeedValidation(
            CommunityFeedValidationException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "COM-001", ex.getMessage(), request);
    }

    @ExceptionHandler(com.carebridge.backend.community.exception.QuestionNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotEditable(
            com.carebridge.backend.community.exception.QuestionNotEditableException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "COM-010", ex.getMessage(), request);
    }

    @ExceptionHandler(com.carebridge.backend.community.exception.QuestionLockedException.class)
    public ResponseEntity<ErrorResponse> handleQuestionLocked(
            com.carebridge.backend.community.exception.QuestionLockedException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "COM-012", ex.getMessage(), request);
    }

    @ExceptionHandler(com.carebridge.backend.community.exception.AnswerNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleAnswerNotEditable(
            com.carebridge.backend.community.exception.AnswerNotEditableException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "COM-013", ex.getMessage(), request);
    }

    @ExceptionHandler(com.carebridge.backend.community.exception.TopicHiddenException.class)
    public ResponseEntity<ErrorResponse> handleTopicHidden(
            com.carebridge.backend.community.exception.TopicHiddenException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "COM-014", ex.getMessage(), request);
    }

    @ExceptionHandler(ContentException.class)
    public ResponseEntity<ErrorResponse> handleContent(ContentException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(com.carebridge.backend.directchat.exception.DirectChatException.class)
    public ResponseEntity<ErrorResponse> handleDirectChat(
            com.carebridge.backend.directchat.exception.DirectChatException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ModerationException.class)
    public ResponseEntity<ErrorResponse> handleModeration(ModerationException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(PartnerException.class)
    public ResponseEntity<ErrorResponse> handlePartner(PartnerException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(RagException.class)
    public ResponseEntity<ErrorResponse> handleRag(RagException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(SafetyException.class)
    public ResponseEntity<ErrorResponse> handleSafety(SafetyException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleExerciseNotFound(
            ExerciseNotFoundException ex, HttpServletRequest request) {
        logger.error("Exercise not found: {}", ex.getMessage());
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidExerciseStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExerciseState(
            InvalidExerciseStateException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(PostureConfigNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostureConfigNotFound(
            PostureConfigNotFoundException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPostureConfigException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPostureConfig(
            InvalidPostureConfigException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(SafetyCheckNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSafetyCheckNotFound(
            SafetyCheckNotFoundException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(SafetyCheckNotClearedException.class)
    public ResponseEntity<ErrorResponse> handleSafetyCheckNotCleared(
            SafetyCheckNotClearedException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateSessionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSession(
            DuplicateSessionException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidSessionStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSessionState(
            InvalidSessionStateException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(SessionOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleSessionOwnership(
            SessionOwnershipException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(SessionNotCompletedException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotCompleted(
            SessionNotCompletedException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
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

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ErrorResponse> handleAccountSuspended(AccountSuspendedException ex, HttpServletRequest request) {
        logger.error("Account suspended: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorization(AuthorizationException ex, HttpServletRequest request) {
        logger.error("Authorization denied: {}", ex.getMessage(), ex);
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
        logger.error("Business access denied: {}", ex.getMessage(), ex);
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(TriageException.class)
    public ResponseEntity<ErrorResponse> handleTriage(TriageException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(RedFlagRuleException.class)
    public ResponseEntity<ErrorResponse> handleRedFlagRule(RedFlagRuleException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(EmergencyException.class)
    public ResponseEntity<ErrorResponse> handleEmergency(EmergencyException ex, HttpServletRequest request) {
        return error(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), request);
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
