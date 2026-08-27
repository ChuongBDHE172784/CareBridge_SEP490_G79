package com.carebridge.backend.content.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * UC-14 Report Content or Account error codes (CB-MOD-IMP-014 §10 — prefix RPT-).
 * Caught by the generic {@code BusinessException} handler in {@code GlobalExceptionHandler}.
 */
public class ReportException extends BusinessException {

    public ReportException(HttpStatus httpStatus, String code, String message) {
        super(httpStatus, code, message);
    }

    public static ReportException targetNotFound(String targetId) {
        return new ReportException(HttpStatus.NOT_FOUND, "RPT-002",
                "Report target not found: " + targetId);
    }

    public static ReportException rateLimitExceeded() {
        return new ReportException(HttpStatus.TOO_MANY_REQUESTS, "RPT-003",
                "You have submitted too many reports for this target in the last 24 hours");
    }

    public static ReportException duplicatePending() {
        return new ReportException(HttpStatus.CONFLICT, "RPT-004",
                "You already have a pending report for this target");
    }

    public static ReportException cannotReportOwnTarget() {
        return new ReportException(HttpStatus.FORBIDDEN, "RPT-005",
                "You cannot report your own content or account");
    }
}
