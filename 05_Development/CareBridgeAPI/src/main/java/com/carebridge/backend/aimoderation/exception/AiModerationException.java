package com.carebridge.backend.aimoderation.exception;

import org.springframework.http.HttpStatus;

/** Domain exception for the AI moderation subsystem — same code+httpStatus shape as ModerationException. */
public class AiModerationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public AiModerationException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static AiModerationException policyNotFound(String id) {
        return new AiModerationException("AIM-001", "AI moderation policy not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static AiModerationException duplicatePolicyCode(String code) {
        return new AiModerationException("AIM-002", "AI moderation policy code already exists: " + code,
                HttpStatus.CONFLICT);
    }

    public static AiModerationException invalidTargetTypes(String value) {
        return new AiModerationException("AIM-003",
                "applicableTargetTypes must be a non-empty subset of QUESTION,ANSWER — got: " + value,
                HttpStatus.BAD_REQUEST);
    }

    public static AiModerationException invalidConfidenceThreshold() {
        return new AiModerationException("AIM-004", "confidenceThreshold must be between 0 and 1",
                HttpStatus.BAD_REQUEST);
    }

    public static AiModerationException systemDefaultCodeImmutable(String code) {
        return new AiModerationException("AIM-005",
                "policyCode of a system-default policy cannot be changed: " + code, HttpStatus.BAD_REQUEST);
    }

    public static AiModerationException guidanceTooLong(int max) {
        return new AiModerationException("AIM-006",
                "detectionGuidance exceeds the maximum length of " + max + " characters", HttpStatus.BAD_REQUEST);
    }

    public static AiModerationException assessmentNotFound(String id) {
        return new AiModerationException("AIM-007", "AI assessment not found for: " + id, HttpStatus.NOT_FOUND);
    }

    public static AiModerationException sandboxUnavailable(String state) {
        return new AiModerationException("AIM-009",
                "AI moderation sandbox is unavailable — Gemini state: " + state, HttpStatus.CONFLICT);
    }

    public static AiModerationException rescanUnsupportedTarget(String targetType) {
        return new AiModerationException("AIM-010",
                "Rescan supports QUESTION, ANSWER or CONTENT targets only — got: " + targetType,
                HttpStatus.BAD_REQUEST);
    }

    public static AiModerationException rescanTargetNotFound(String targetId) {
        return new AiModerationException("AIM-011",
                "Rescan target does not exist or is no longer scannable: " + targetId, HttpStatus.NOT_FOUND);
    }

    public static AiModerationException feedbackRequiresAttachedCase(String assessmentId) {
        return new AiModerationException("AIM-013",
                "Feedback is only possible for an assessment attached to a moderation case: " + assessmentId,
                HttpStatus.CONFLICT);
    }
}
