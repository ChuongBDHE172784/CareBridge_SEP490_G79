package com.carebridge.backend.triage.exception;

import org.springframework.http.HttpStatus;

public class RedFlagRuleException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public RedFlagRuleException(String code, String message, HttpStatus httpStatus) {
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

    public static RedFlagRuleException duplicateKeyword() {
        return new RedFlagRuleException(
                "MOD-025",
                "A red-flag rule with this keyword already exists",
                HttpStatus.CONFLICT);
    }

    public static RedFlagRuleException ruleNotFound() {
        return new RedFlagRuleException(
                "MOD-026",
                "Red-flag rule not found",
                HttpStatus.NOT_FOUND);
    }

    public static RedFlagRuleException systemDefaultProtected() {
        return new RedFlagRuleException(
                "MOD-027",
                "Cannot delete or deactivate a system-default red-flag rule",
                HttpStatus.CONFLICT);
    }
}
