package com.carebridge.backend.content.exception;

import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ModerationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ModerationException(String code, String message, HttpStatus httpStatus) {
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

    public static ModerationException pageSizeExceeded() {
        return new ModerationException(
                "MOD-002",
                "Page size exceeds maximum allowed value of 50",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException internalError() {
        return new ModerationException(
                "MOD-005",
                "Internal moderation service error",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // UC-100 (CB-MOD-IMP-002 §11.3 Chặng 2)
    public static ModerationException targetNotFound(UUID targetId, ReportTargetType targetType) {
        return new ModerationException(
                "MOD-007",
                "Target " + targetType + " with id " + targetId + " not found",
                HttpStatus.NOT_FOUND);
    }

    public static ModerationException actionNotSupportedForTargetType(
            ModerationActionType actionType, ReportTargetType targetType) {
        return new ModerationException(
                "MOD-008",
                "Action " + actionType + " is not supported for target type " + targetType,
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException unsupportedActionType(ModerationActionType actionType) {
        return new ModerationException(
                "MOD-009",
                "Action type " + actionType + " is not supported by this endpoint",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException reasonRequired(ModerationActionType actionType) {
        return new ModerationException(
                "MOD-010",
                "reason is required for action type " + actionType,
                HttpStatus.BAD_REQUEST);
    }

    // UC-101 (CB-MOD-IMP-003 §11.3 Chặng 2)
    public static ModerationException reportNotFound(UUID reportId) {
        return new ModerationException(
                "MOD-003",
                "Report not found",
                HttpStatus.NOT_FOUND);
    }

    public static ModerationException reportAlreadyResolved(UUID reportId) {
        return new ModerationException(
                "MOD-011",
                "Report " + reportId + " has already been resolved/dismissed",
                HttpStatus.CONFLICT);
    }

    public static ModerationException contentActionNotSupportedForReport() {
        return new ModerationException(
                "MOD-012",
                "Only DISMISS is supported for targetType=CONTENT via this endpoint",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException accountActionNotAvailable(ModerationActionType actionType) {
        return new ModerationException(
                "MOD-013",
                "Account action " + actionType + " is not supported for this report target",
                HttpStatus.BAD_REQUEST);
    }

    // UC-102 (CB-MOD-IMP-004 §11.3 Chặng 3)
    public static ModerationException targetUserNotFound(UUID targetUserId) {
        return new ModerationException(
                "MOD-015",
                "Target user with id " + targetUserId + " not found",
                HttpStatus.NOT_FOUND);
    }

    public static ModerationException accountActionTypeNotSupported(ModerationActionType actionType) {
        return new ModerationException(
                "MOD-016",
                "Action type " + actionType + " is not supported by this endpoint",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException accountReasonRequired(ModerationActionType actionType) {
        return new ModerationException(
                "MOD-017",
                "reason is required for action type " + actionType,
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException suspendExpiresAtInvalid() {
        return new ModerationException(
                "MOD-018",
                "expiresAt is required and must be in the future for action type SUSPEND",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException restrictExpiresAtInvalid() {
        return new ModerationException(
                "MOD-024",
                "expiresAt is required and must be in the future for action type RESTRICT",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException warnExpiresAtNotAllowed() {
        return new ModerationException(
                "MOD-019",
                "expiresAt must not be provided for action type WARN",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException selfActionForbidden() {
        return new ModerationException(
                "MOD-020",
                "Moderators cannot warn or suspend their own account",
                HttpStatus.BAD_REQUEST);
    }

    // UC-111 (CB-MOD-IMP-006 §10)
    public static ModerationException invalidDateRange() {
        return new ModerationException(
                "MOD-021",
                "Invalid date range: 'from' must not be after 'to'",
                HttpStatus.BAD_REQUEST);
    }

    // UC-113 (CB-MOD-IMP-007 §10)
    public static ModerationException invalidImpactReportDateRange() {
        return new ModerationException(
                "MOD-022",
                "Invalid date range: 'from' must not be after 'to'",
                HttpStatus.BAD_REQUEST);
    }

    // Pending Content Queue (CB-MOD-IMP-004 §9, ADR-006)
    public static ModerationException pendingContentTargetTypeUnsupported(ReportTargetType targetType) {
        return new ModerationException(
                "MOD-023",
                "targetType must be QUESTION or ANSWER for pending-content queue, got " + targetType,
                HttpStatus.BAD_REQUEST);
    }

    // CB-MOD-IMP-009 (Undo Moderation Action, §10)
    public static ModerationException moderationActionNotFound(java.util.UUID actionId) {
        return new ModerationException(
                "MOD-025",
                "Moderation action " + actionId + " not found",
                HttpStatus.NOT_FOUND);
    }

    public static ModerationException undoTargetTypeUnsupported(ReportTargetType targetType) {
        return new ModerationException(
                "MOD-026",
                "Undo is only supported for targetType QUESTION or ANSWER, got " + targetType,
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException undoNotSupportedForReportResolution(java.util.UUID actionId) {
        return new ModerationException(
                "MOD-027",
                "Action " + actionId + " originated from a report resolution — undo is not supported for that path",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException undoActionTypeNotSupported(ModerationActionType actionType) {
        return new ModerationException(
                "MOD-028",
                "Action type " + actionType + " cannot be undone",
                HttpStatus.BAD_REQUEST);
    }

    public static ModerationException undoNotMostRecentAction(java.util.UUID actionId) {
        return new ModerationException(
                "MOD-029",
                "Action " + actionId + " is not the most recent action for this target — a newer action already exists",
                HttpStatus.CONFLICT);
    }

    public static ModerationException undoStatusSuperseded(java.util.UUID actionId) {
        return new ModerationException(
                "MOD-030",
                "Current status no longer matches the result of action " + actionId + " — it may have been superseded",
                HttpStatus.CONFLICT);
    }
}
