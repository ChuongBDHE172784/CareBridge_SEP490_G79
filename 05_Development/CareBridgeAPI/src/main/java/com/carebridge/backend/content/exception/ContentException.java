package com.carebridge.backend.content.exception;

import org.springframework.http.HttpStatus;

public class ContentException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public ContentException(String code, String message, HttpStatus httpStatus) {
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

    public static ContentException duplicateContent() {
        return new ContentException(
                "CNT-002",
                "Content with same title, stage and type already exists",
                HttpStatus.CONFLICT);
    }

    public static ContentException topicNotFound(String topicId) {
        return new ContentException(
                "CNT-003",
                "Topic not found: " + topicId,
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException contentNotFound() {
        return new ContentException(
                "CNT-003",
                "Content not found or not available",
                HttpStatus.NOT_FOUND);
    }

    public static ContentException validationFailed(String field, String message) {
        return new ContentException(
                "CNT-001",
                "Validation failed: " + field + " - " + message,
                HttpStatus.BAD_REQUEST);
    }

    // UC-107 (CB-CONTENT-IMP-006 §11.3)
    public static ContentException alreadyArchived() {
        return new ContentException(
                "CNT-006",
                "Content item is already archived",
                HttpStatus.CONFLICT);
    }

    public static ContentException hideReasonRequired() {
        return new ContentException(
                "CNT-007",
                "Reason is required to hide content",
                HttpStatus.BAD_REQUEST);
    }

    // UC-108 (CB-CONTENT-IMP-005 §10)
    public static ContentException notPendingReview() {
        return new ContentException(
                "CNT-008",
                "Content item is not pending review",
                HttpStatus.CONFLICT);
    }

    public static ContentException decisionReasonRequired() {
        return new ContentException(
                "CNT-009",
                "Reason required for REJECT",
                HttpStatus.BAD_REQUEST);
    }

    public static ContentException notCurrentlyPublished() {
        return new ContentException("CNT-010", "Content item is not currently published", HttpStatus.CONFLICT);
    }

    public static ContentException unpublishReasonRequired() {
        return new ContentException("CNT-011", "Reason required for unpublish", HttpStatus.BAD_REQUEST);
    }

    public static ContentException invalidContentStatusTransition() {
        return new ContentException(
                "CNT-012",
                "Content Admin may only save a draft or submit it for review",
                HttpStatus.CONFLICT);
    }
}
